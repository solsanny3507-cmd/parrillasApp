package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.repository.TransaccionLockRepository;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoTransaccion;
import ec.gob.epmmop.parrillasapp.placetopay.config.PagoException;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.ResultadoProcesamientoPagoDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.OrigenProcesamientoPago;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class AplicacionEstadoPagoService extends Utilitarios {

    private static final Logger LOGGER = Logger.getLogger(AplicacionEstadoPagoService.class.getName());

    @Inject
    private TransaccionLockRepository transaccionLockRepository;

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private ProcesoPostPagoService procesoPostPagoService;

    /**
     * Sección crítica del procesamiento.
     * <p>
     * Esta transacción:
     * <p>
     * 1. Bloquea la fila en PostgreSQL.
     * 2. Vuelve a comprobar su estado.
     * 3. Solo permite que un proceso aplique el cambio.
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public ResultadoProcesamientoPagoDTO aplicarEstado(Integer requestId, String reference, PagoResponseDTO pagoResponseDTO, OrigenProcesamientoPago origen) {

        Optional<Transaccion> optional = transaccionLockRepository.obtenerParaProcesar(requestId, reference);

        if (optional.isEmpty()) {

            throw new PagoException("No existe la transacción para requestId=" + requestId + " y reference=" + reference);
        }

        /*
         * A partir de este punto tenemos SELECT ... FOR UPDATE.
         *
         * Otro Webhook / Return / Cron que intente procesar
         * exactamente esta transacción tendrá que esperar.
         */
        Transaccion transaccion = optional.get();

        String estadoActual = transaccion.getTrxEstadoTransaccion();

        String nuevoEstado = transformarStatusReserva(pagoResponseDTO.getStatus());

        LOGGER.info("Validando transición de pago" + " | requestId=" + requestId + " | origen=" + origen + " | actual=" + estadoActual + " | nuevo=" + nuevoEstado);

        /*
         * --------------------------------------------------
         * CASO 1
         * La transacción ya fue finalizada anteriormente.
         * --------------------------------------------------
         */
        if (esEstadoFinal(estadoActual)) {

            if (!estadoActual.equalsIgnoreCase(nuevoEstado)) {

                LOGGER.severe("INCONSISTENCIA DE ESTADO" + " | requestId=" + requestId + " | BD=" + estadoActual + " | PlaceToPay=" + nuevoEstado + " | origen=" + origen + " | NO SE MODIFICARA LA TRANSACCION");

            } else {

                LOGGER.info("Transacción ya procesada anteriormente" + " | requestId=" + requestId + " | estado=" + estadoActual + " | origen=" + origen);
            }

            return new ResultadoProcesamientoPagoDTO(transaccion, pagoResponseDTO, false);
        }

        /*
         * --------------------------------------------------
         * CASO 2
         * PlaceToPay todavía indica PENDING.
         *
         * No necesitamos volver a guardar nada.
         * --------------------------------------------------
         */
        if (EstadoTransaccion.PENDIENTE.getDescripcion().equalsIgnoreCase(nuevoEstado)) {

            LOGGER.info("Pago todavía pendiente" + " | requestId=" + requestId + " | origen=" + origen);

            return new ResultadoProcesamientoPagoDTO(transaccion, pagoResponseDTO, false);
        }

        /*
         * --------------------------------------------------
         * CASO 3
         * Solo permitimos llegar aquí para estados finales.
         * --------------------------------------------------
         */
        if (!esEstadoFinal(nuevoEstado)) {

            throw new PagoException("No se permite procesar el estado: " + nuevoEstado + " para requestId=" + requestId);
        }

        /*
         * Permitimos recuperación desde ERROR porque ERROR
         * representa un problema técnico local y no debe
         * considerarse un resultado financiero definitivo.
         */
        boolean estadoProcesable = EstadoTransaccion.PENDIENTE.getDescripcion().equalsIgnoreCase(estadoActual) || EstadoTransaccion.ERROR.getDescripcion().equalsIgnoreCase(estadoActual);

        if (!estadoProcesable) {

            throw new PagoException("No se permite la transición " + estadoActual + " -> " + nuevoEstado + " para requestId=" + requestId);
        }

        /*
         * ESTE ES EL ÚNICO PUNTO QUE DEBE MODIFICAR
         * TRANSACCIÓN + RESERVA COMO RESPUESTA AL PAGO.
         */
        Transaccion actualizada = transaccionService.modificarTransaccionReserva(transaccion, pagoResponseDTO);

        if (EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(actualizada.getTrxEstadoTransaccion())) {
            procesoPostPagoService.registrarPendiente(actualizada, pagoResponseDTO);
        }

        LOGGER.info("Pago aplicado correctamente" + " | requestId=" + requestId + " | estado=" + actualizada.getTrxEstadoTransaccion() + " | origen=" + origen);

        return new ResultadoProcesamientoPagoDTO(actualizada, pagoResponseDTO, true);
    }

    private boolean esEstadoFinal(String estado) {

        if (estado == null) {
            return false;
        }

        return EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(estado) || EstadoTransaccion.RECHAZADO.getDescripcion().equalsIgnoreCase(estado);
    }
}
