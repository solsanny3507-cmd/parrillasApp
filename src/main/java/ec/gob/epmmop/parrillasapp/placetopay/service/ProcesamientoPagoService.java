package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoTransaccion;
import ec.gob.epmmop.parrillasapp.placetopay.config.PagoException;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.ResultadoProcesamientoPagoDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.OrigenProcesamientoPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.ResultadoAuditoriaPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.TipoEventoPago;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.logging.Logger;

/**
 * ÚNICO punto de entrada para procesar estados de pago.
 *
 * WEBHOOK, RETORNO_COMERCIO y CRON deben utilizar
 * exclusivamente este servicio.
 */
@ApplicationScoped
public class ProcesamientoPagoService extends Utilitarios {

    private static final Logger LOGGER = Logger.getLogger(ProcesamientoPagoService.class.getName());

    private static final Set<String> ESTADOS_VALIDOS = Set.of("APPROVED", "REJECTED", "PENDING");

    @Inject
    private PlacetoPayClient placetoPayClient;

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private AplicacionEstadoPagoService aplicacionEstadoPagoService;

    @Inject
    private PostPagoAsyncDispatcher postPagoAsyncDispatcher;

    @Inject
    private AuditoriaPagoService auditoriaPagoService;

    public ResultadoProcesamientoPagoDTO procesar(Integer requestId, String reference, OrigenProcesamientoPago origen) {

        String referenciaAuditoria = reference;

        String estadoInicial = null;

        TipoEventoPago tipoError = TipoEventoPago.ERROR_PROCESAMIENTO;

        try {

            validarRequestId(requestId);

            /*
             * =====================================================
             * 1. OBTENER TRANSACCIÓN LOCAL
             * =====================================================
             */

            Transaccion transaccion = obtenerTransaccion(requestId, reference);

            if (transaccion == null) {

                throw new PagoException("No existe transacción para requestId=" + requestId);
            }

            String referenceReal = transaccion.getTrxReferencia();

            referenciaAuditoria = referenceReal;

            estadoInicial = transaccion.getTrxEstadoTransaccion();

            if (referenceReal == null || referenceReal.isBlank()) {

                throw new PagoException("La transacción no posee reference" + " | requestId=" + requestId);
            }

            if (reference != null && !reference.isBlank() && !referenceReal.equals(reference)) {

                throw new PagoException("La referencia recibida " + "no coincide con la registrada");
            }

            /*
             * =====================================================
             * 2. AUDITAR INICIO DE CONSULTA
             * =====================================================
             */

            auditoriaPagoService.registrar(requestId, referenceReal, origen, TipoEventoPago.CONSULTA_INICIADA, null, estadoInicial, null, ResultadoAuditoriaPago.OK, "Inicio de consulta de estado a PlaceToPay", null);

            LOGGER.info("Consultando PlaceToPay" + " | requestId=" + requestId + " | reference=" + referenceReal + " | origen=" + origen);

            /*
             * Desde aquí cualquier excepción es,
             * inicialmente, una falla de consulta.
             */
            tipoError = TipoEventoPago.ERROR_CONSULTA;

            /*
             * =====================================================
             * 3. CONSULTAR PLACE TO PAY
             * =====================================================
             */

            String auth = placetoPayClient.generarJsonConsulta();

            if (auth == null || auth.isBlank() || "{}".equals(auth.trim())) {

                throw new PagoException("No fue posible generar autenticación " + "para consultar PlaceToPay");
            }

            PagoResponseDTO pagoResponseDTO = placetoPayClient.consultarPago(requestId, auth);

            validarRespuesta(pagoResponseDTO, requestId);

            validarReferenciaPlaceToPay(transaccion, pagoResponseDTO);

            /*
             * =====================================================
             * 4. AUDITAR RESPUESTA DE PLACE TO PAY
             * =====================================================
             */

            auditoriaPagoService.registrar(requestId, referenceReal, origen, TipoEventoPago.ESTADO_RECIBIDO, pagoResponseDTO.getStatus(), estadoInicial, null, ResultadoAuditoriaPago.OK, pagoResponseDTO.getMessage(), null);

            /*
             * A partir de aquí la consulta fue correcta.
             * Cualquier excepción corresponde al procesamiento
             * local del resultado.
             */
            tipoError = TipoEventoPago.ERROR_PROCESAMIENTO;

            /*
             * =====================================================
             * 5. APLICAR ESTADO BAJO LOCK
             * =====================================================
             */

            ResultadoProcesamientoPagoDTO resultado = aplicacionEstadoPagoService.aplicarEstado(requestId, referenceReal, pagoResponseDTO, origen);

            /*
             * =====================================================
             * 6. AUDITAR RESULTADO DESPUÉS DEL COMMIT
             * =====================================================
             */

            auditarResultadoAplicacion(requestId, referenceReal, origen, estadoInicial, pagoResponseDTO, resultado);

            /*
             * =====================================================
             * 7. POSTPROCESO
             * =====================================================
             */

            if (resultado.isActualizado() && EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(resultado.getTransaccion().getTrxEstadoTransaccion())) {

                postPagoAsyncDispatcher.procesar(requestId, referenceReal);
            }

            return resultado;

        } catch (Exception e) {

            /*
             * =====================================================
             * AUDITORÍA DE ERROR
             * =====================================================
             */

            auditoriaPagoService.registrar(requestId, referenciaAuditoria, origen, tipoError, null, estadoInicial, null, ResultadoAuditoriaPago.ERROR, tipoError == TipoEventoPago.ERROR_CONSULTA ? "Error consultando estado en PlaceToPay" : "Error procesando el estado del pago", e);

            /*
             * Importante:
             * Conservamos el comportamiento original.
             */
            throw e;
        }
    }

    private void auditarResultadoAplicacion(
            Integer requestId,
            String reference,
            OrigenProcesamientoPago origen,
            String estadoAnterior,
            PagoResponseDTO pagoResponseDTO,
            ResultadoProcesamientoPagoDTO resultado) {

        if (resultado == null
            || resultado.getTransaccion() == null) {

            return;
        }

        String estadoActual =
                resultado
                        .getTransaccion()
                        .getTrxEstadoTransaccion();

        String estadoPlaceToPayTraducido =
                transformarStatusReserva(
                        pagoResponseDTO.getStatus()
                );

        /*
         * =====================================================
         * ESTADO REALMENTE APLICADO
         * =====================================================
         */

        if (resultado.isActualizado()) {

            auditoriaPagoService.registrar(
                    requestId,
                    reference,
                    origen,
                    TipoEventoPago.ESTADO_APLICADO,
                    pagoResponseDTO.getStatus(),
                    estadoAnterior,
                    estadoActual,
                    ResultadoAuditoriaPago.OK,
                    "Estado del pago aplicado correctamente",
                    null
            );

            return;
        }

        /*
         * =====================================================
         * TRANSACCIÓN YA ESTABA EN ESTADO FINAL
         * =====================================================
         */

        if (esEstadoFinal(
                estadoActual
        )) {

            if (estadoActual.equalsIgnoreCase(
                    estadoPlaceToPayTraducido
            )) {

                auditoriaPagoService.registrar(
                        requestId,
                        reference,
                        origen,
                        TipoEventoPago.YA_PROCESADO,
                        pagoResponseDTO.getStatus(),
                        estadoActual,
                        estadoActual,
                        ResultadoAuditoriaPago.OMITIDO,
                        "La transacción ya había sido procesada",
                        null
                );

            } else {

                auditoriaPagoService.registrar(
                        requestId,
                        reference,
                        origen,
                        TipoEventoPago.ESTADO_INCONSISTENTE,
                        pagoResponseDTO.getStatus(),
                        estadoActual,
                        estadoPlaceToPayTraducido,
                        ResultadoAuditoriaPago.ADVERTENCIA,
                        "PlaceToPay devolvió un estado diferente "
                        + "al estado final almacenado localmente",
                        null
                );
            }

            return;
        }

        /*
         * =====================================================
         * PENDING → PENDING
         * =====================================================
         */

        auditoriaPagoService.registrar(
                requestId,
                reference,
                origen,
                TipoEventoPago.SIN_CAMBIO,
                pagoResponseDTO.getStatus(),
                estadoActual,
                estadoActual,
                ResultadoAuditoriaPago.OMITIDO,
                "El pago continúa sin un estado financiero final",
                null
        );
    }

    private boolean esEstadoFinal(String estado) {

        if (estado == null) {
            return false;
        }

        return EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(estado) || EstadoTransaccion.RECHAZADO.getDescripcion().equalsIgnoreCase(estado);
    }

    private void validarRequestId(Integer requestId) {

        if (requestId == null) {

            throw new PagoException("El requestId no puede ser null");
        }
    }

    private Transaccion obtenerTransaccion(Integer requestId, String reference) {

        if (reference != null && !reference.isBlank()) {

            return transaccionService.obtenerTransaccion(requestId, reference);
        }

        return transaccionService.obtenerTransaccion(requestId);
    }

    private void validarRespuesta(PagoResponseDTO pago, Integer requestId) {

        if (pago == null) {

            throw new PagoException("PlaceToPay no devolvió respuesta" + " | requestId=" + requestId);
        }

        String status = pago.getStatus();

        if (status == null || !ESTADOS_VALIDOS.contains(status.toUpperCase())) {

            /*
             * ERROR técnico != REJECTED.
             *
             * Bajo ninguna circunstancia debemos cancelar
             * una reserva porque PlaceToPay no respondió.
             */
            throw new PagoException("Estado no procesable de PlaceToPay" + " | requestId=" + requestId + " | status=" + status + " | message=" + pago.getMessage());
        }
    }

    private void validarReferenciaPlaceToPay(Transaccion transaccion, PagoResponseDTO pago) {

        String referenciaBD = transaccion.getTrxReferencia();

        String referenciaPlaceToPay = pago.getReference();

        if (referenciaPlaceToPay == null || referenciaPlaceToPay.isBlank()) {

            throw new PagoException("PlaceToPay no devolvió reference");
        }

        if (!referenciaBD.equals(referenciaPlaceToPay)) {

            throw new PagoException("La referencia de PlaceToPay no coincide con la transacción");
        }
    }
}
