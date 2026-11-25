package ec.gob.epmmop.parrillasapp.placetopay.service;

import com.google.zxing.WriterException;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.FacturaService;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.miquito.model.ResultadoNotificacionMiQuito;
import ec.gob.epmmop.parrillasapp.miquito.service.MiQuitoNotificacionService;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoTransaccion;
import ec.gob.epmmop.parrillasapp.parrillas.service.MailService;
import ec.gob.epmmop.parrillasapp.parrillas.service.QRCodeService;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.EstadoProcesoPostPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.ProcesoPostPago;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ejecuta procesos secundarios posteriores al COMMIT
 * del pago.
 *
 * Ninguna falla en QR, factura o correo debe modificar
 * el estado financiero de la transacción.
 */
@ApplicationScoped
public class PostPagoService extends Utilitarios {

    private static final Logger LOGGER = Logger.getLogger(PostPagoService.class.getName());

    @Inject
    private ProcesoPostPagoService procesoPostPagoService;

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private FacturaService facturaService;

    @Inject
    private QRCodeService qrCodeService;

    @Inject
    private MailService mailService;

    @Inject
    private MiQuitoNotificacionService miQuitoNotificacionService;

    public void procesar(Integer requestId, String reference) {

        ProcesoPostPago proceso = procesoPostPagoService.obtener(requestId, reference);

        if (proceso == null) {
            return;
        }

        procesar(proceso.getSecuencial());
    }

    public void procesar(Long procesoId) {

        boolean reclamado = procesoPostPagoService.reclamar(procesoId);

        if (!reclamado) {

            LOGGER.info("PostPago ya está siendo procesado" + " | id=" + procesoId);

            return;
        }

        try {

            ProcesoPostPago proceso = procesoPostPagoService.obtener(procesoId);

            if (proceso == null) {
                return;
            }

            Transaccion transaccion = transaccionService.obtenerTransaccionCompleta(proceso.getRequestId(), proceso.getReference());

            if (transaccion == null) {

                throw new IllegalStateException("No existe transacción para proceso postpago " + procesoId);
            }

            /*
             * Protección adicional:
             *
             * NUNCA procesamos factura, QR ni correo
             * si el pago local no está APROBADO.
             */
            if (!EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(transaccion.getTrxEstadoTransaccion())) {

                LOGGER.warning("Se omite PostPago porque la transacción" + " no está APROBADA" + " | requestId=" + proceso.getRequestId());

                return;
            }

            PagoResponseDTO pago = reconstruirPago(proceso);

            /*
             * Los tres procesos son INDEPENDIENTES.
             *
             * Una excepción en uno NO impide ejecutar
             * los otros.
             */

            procesarQr(proceso, transaccion, pago);

            procesarFactura(proceso, transaccion, pago);

            /*
             * Recargamos porque el QR puede haberse
             * guardado en una transacción independiente.
             */
            transaccion = transaccionService.obtenerTransaccionCompleta(proceso.getRequestId(), proceso.getReference());

            procesarCorreo(proceso, transaccion);

            procesarNotificacionMiQuito(proceso, transaccion);

        } finally {

            procesoPostPagoService.liberar(procesoId);
        }
    }

    // ==========================================================
    // QR
    // ==========================================================

    private void procesarQr(ProcesoPostPago proceso, Transaccion transaccion, PagoResponseDTO pago) {

        if (EstadoProcesoPostPago.COMPLETADO.name().equals(proceso.getEstadoQr())) {

            return;
        }

        try {

            String contenido = qrCodeService.construirContenidoQR(transaccion.getReservaParrilla(), pago.getAuthorization());

            byte[] qr = qrCodeService.generarQR(contenido, 300, 300);

            procesoPostPagoService.qrCompletado(proceso.getSecuencial(), proceso.getRequestId(), proceso.getReference(), qr);

            LOGGER.info("QR generado" + " | requestId=" + proceso.getRequestId());

        } catch (WriterException | IOException | RuntimeException e) {

            procesoPostPagoService.qrError(proceso.getSecuencial(), e.getMessage());

            LOGGER.log(Level.SEVERE, "Error generando QR" + " | requestId=" + proceso.getRequestId(), e);
        }
    }

    // ==========================================================
    // FACTURA
    // ==========================================================

    private void procesarFactura(ProcesoPostPago proceso, Transaccion transaccion, PagoResponseDTO pago) {

        if (EstadoProcesoPostPago.COMPLETADO.name().equals(proceso.getEstadoFactura())) {

            return;
        }

        try {

            /*
             * Conservamos tu servicio actual.
             *
             * La diferencia fundamental es que ahora
             * esta llamada ocurre DESPUÉS DEL COMMIT
             * del pago.
             */
            facturaService.enviarFactura(transaccion, pago);

            procesoPostPagoService.facturaCompletada(proceso.getSecuencial());

            LOGGER.info("Factura procesada" + " | requestId=" + proceso.getRequestId());

        } catch (Exception e) {

            procesoPostPagoService.facturaError(proceso.getSecuencial(), e.getMessage());

            LOGGER.log(Level.SEVERE, "Error facturando" + " | requestId=" + proceso.getRequestId(), e);
        }
    }

    // ==========================================================
    // CORREO
    // ==========================================================

    private void procesarCorreo(ProcesoPostPago proceso, Transaccion transaccion) {

        if (EstadoProcesoPostPago.COMPLETADO.name().equals(proceso.getEstadoCorreo())) {

            return;
        }

        byte[] qr = transaccion.getTrxCodigoQR();

        /*
         * El correo depende del QR.
         *
         * Si aún no existe QR dejamos CORREO=PENDIENTE.
         * NO generamos error.
         */
        if (qr == null || qr.length == 0) {

            LOGGER.info("Correo pendiente porque todavía" + " no existe QR" + " | requestId=" + proceso.getRequestId());

            return;
        }

        try {

            String htmlCorreo = construirHtmlReserva(transaccion);

            String correoCliente = transaccion.getCiudadano().getCiuCorreo();

            String asunto = "Confirmación de tu reserva - " + transaccion.getReservaParrilla().getParrilla().getAsaNombre() + " en Parque " + transaccion.getReservaParrilla().getParrilla().getGrupoParrilla().getParque().getParNombre();

            mailService.enviarHtmlConQR(correoCliente, asunto, htmlCorreo, qr);

            procesoPostPagoService.correoCompletado(proceso.getSecuencial());

            LOGGER.info("Correo enviado" + " | requestId=" + proceso.getRequestId());

        } catch (Exception e) {

            /*
             * ESTO JAMÁS MODIFICA EL PAGO.
             */
            procesoPostPagoService.correoError(proceso.getSecuencial(), e.getMessage());

            LOGGER.log(Level.SEVERE, "Error enviando correo" + " | requestId=" + proceso.getRequestId(), e);
        }
    }

    private PagoResponseDTO reconstruirPago(ProcesoPostPago proceso) {

        PagoResponseDTO dto = new PagoResponseDTO();

        dto.setStatus(proceso.getStatusPlaceToPay());

        dto.setAuthorization(proceso.getAuthorization());

        dto.setNombreTarjeta(proceso.getNombreTarjeta());

        dto.setReference(proceso.getReference());

        dto.setReason(proceso.getReason());

        dto.setMessage(proceso.getMessage());

        dto.setDate(proceso.getFechaPago());

        return dto;
    }

    // ==========================================================
    // NOTIFICACIÓN MI QUITO
    // ==========================================================

    private void procesarNotificacionMiQuito(ProcesoPostPago proceso, Transaccion transaccion) {

        /*
         * La notificación se intenta UNA SOLA VEZ.
         *
         * COMPLETADO, SIN_SESION, ERROR y NO_APLICA
         * son estados terminales para este proceso.
         */
        if (!EstadoProcesoPostPago.PENDIENTE.name().equals(proceso.getEstadoNotificacion())) {

            return;
        }

        /*
         * Mi Quito abrirá /buzon y posteriormente
         * consultará nuestra API de reservas activas.
         *
         * Esa API solo expone reservas cuyo QR ya existe,
         * por lo que no enviamos el push antes del QR.
         */
        byte[] qr = transaccion.getTrxCodigoQR();

        if (qr == null || qr.length == 0) {

            LOGGER.info("Notificación Mi Quito pendiente porque " + "todavía no existe QR" + " | requestId=" + proceso.getRequestId());

            return;
        }

        try {

            String identificacion = transaccion.getCiudadano().getCiuIdentificacion();

            String nombreParque = transaccion.getReservaParrilla().getParrilla().getGrupoParrilla().getParque().getParNombre();

            ResultadoNotificacionMiQuito resultado = miQuitoNotificacionService.notificarReservaExitosa(identificacion, nombreParque, proceso.getRequestId());

            switch (resultado) {

                case ENVIADA -> procesoPostPagoService.notificacionCompletada(proceso.getSecuencial());

                case SIN_SESION -> procesoPostPagoService.notificacionSinSesion(proceso.getSecuencial());
            }

        } catch (Exception e) {

            /*
             * Esta falla jamás modifica:
             * - pago
             * - reserva
             * - QR
             * - factura
             * - correo
             */
            procesoPostPagoService.notificacionError(proceso.getSecuencial(), e.getMessage());

            LOGGER.log(Level.SEVERE, "Error notificando a Mi Quito" + " | requestId=" + proceso.getRequestId(), e);
        }
    }
}
