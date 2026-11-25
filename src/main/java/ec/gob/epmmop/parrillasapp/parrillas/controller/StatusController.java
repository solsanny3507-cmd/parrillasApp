package ec.gob.epmmop.parrillasapp.parrillas.controller;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.ResultadoProcesamientoPagoDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.EstadoProcesoPostPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.OrigenProcesamientoPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.ProcesoPostPago;
import ec.gob.epmmop.parrillasapp.placetopay.service.ProcesamientoPagoService;
import ec.gob.epmmop.parrillasapp.placetopay.service.ProcesoPostPagoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Named
@ViewScoped
public class StatusController extends Utilitarios implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(StatusController.class);

    // =========================================================
    // SERVICES
    // =========================================================

    @Inject
    private ProcesamientoPagoService procesamientoPagoService;

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private ProcesoPostPagoService procesoPostPagoService;

    // =========================================================
    // DATOS DEL PAGO
    // =========================================================

    private PagoResponseDTO pagoResponseDTO;

    private Transaccion transaccion;

    private Integer requestId;

    private String status;

    private String message;

    // =========================================================
    // ESTADOS DE LA INTERFAZ
    // =========================================================

    private boolean pagoExitoso;

    private boolean pagoRechazado;

    private boolean pagoPendiente;

    // =========================================================
    // QR
    // =========================================================

    private StreamedContent qrImage;

    private boolean qrDisponible;

    // =========================================================
    // CORREO
    // =========================================================

    private boolean correoEnviado;

    private boolean correoError;

    private boolean correoPendiente;

    // =========================================================
    // INIT
    // =========================================================

    @PostConstruct
    public void init() {

        FacesContext context = FacesContext.getCurrentInstance();

        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();

        try {

            /*
             * Evita que el navegador muestre información
             * antigua cuando el usuario utiliza atrás/adelante.
             */
            context.getExternalContext().setResponseHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            context.getExternalContext().setResponseHeader("Pragma", "no-cache");

            context.getExternalContext().setResponseHeader("Expires", "0");

            Object requestIdObj = sessionMap.get("requestId");

            if (requestIdObj == null) {

                log.warn("No existe requestId en sesión");

                redirigirABooking();

                return;
            }

            this.requestId = Integer.valueOf(requestIdObj.toString());

            this.status = null;

            consultarPagoReserva(this.requestId);

        } catch (Exception e) {

            log.error("Error cargando estado de pago. requestId={}", this.requestId, e);

            redirigirABooking();
        }
    }

    // =========================================================
    // CONSULTA PRINCIPAL
    // =========================================================

    public void consultarPagoReserva(Integer requestId) {

        ResultadoProcesamientoPagoDTO resultado = procesamientoPagoService.procesar(requestId, null, OrigenProcesamientoPago.RETORNO_COMERCIO);

        this.pagoResponseDTO = resultado.getPagoResponse();

        this.transaccion = resultado.getTransaccion();

        this.message = this.pagoResponseDTO != null ? this.pagoResponseDTO.getMessage() : null;

        this.status = this.pagoResponseDTO != null ? transformarStatusReserva(this.pagoResponseDTO.getStatus()) : this.transaccion.getTrxEstadoTransaccion();

        verificarStatusTransaccion(this.transaccion.getTrxEstadoTransaccion());

        /*
         * Comprobamos si el QR ya fue generado.
         *
         * Si todavía no existe, el p:poll de status.xhtml
         * continuará consultando hasta encontrarlo.
         */
        actualizarQrDesdeTransaccion(this.transaccion);

        /*
         * Consultamos el estado actual del correo.
         */
        actualizarEstadoCorreo();
    }

    // =========================================================
    // ESTADO DEL PAGO
    // =========================================================

    public void verificarStatusTransaccion(String statusRespuesta) {

        this.pagoExitoso = false;
        this.pagoRechazado = false;
        this.pagoPendiente = false;

        if (statusRespuesta == null) {
            return;
        }

        switch (statusRespuesta) {

            case APROBADO -> this.pagoExitoso = true;

            case RECHAZADO -> this.pagoRechazado = true;

            case PENDIENTE -> this.pagoPendiente = true;

            default -> this.pagoPendiente = true;
        }
    }

    // =========================================================
    // REFRESCO ASÍNCRONO
    // =========================================================

    /**
     * Es invocado por p:poll mientras el QR todavía
     * no se encuentre disponible.
     * <p>
     * IMPORTANTE:
     * <p>
     * Este método NO consulta PlaceToPay.
     * NO procesa nuevamente el pago.
     * NO factura.
     * NO genera QR.
     * NO envía correo.
     * <p>
     * Solamente consulta PostgreSQL.
     */
    public void refrescarEstadoReserva() {

        try {

            if (!this.pagoExitoso || this.requestId == null || this.transaccion == null) {

                return;
            }

            String reference = this.transaccion.getTrxReferencia();

            Transaccion actualizada;

            /*
             * Recuperamos la transacción completa para
             * evitar problemas con relaciones LAZY cuando
             * JSF renderice parque, parrilla, ciudadano, etc.
             */
            if (reference != null && !reference.isBlank()) {

                actualizada = transaccionService.obtenerTransaccionCompleta(this.requestId, reference);

            } else {

                actualizada = transaccionService.obtenerTransaccion(this.requestId);
            }

            if (actualizada == null) {
                return;
            }

            this.transaccion = actualizada;

            actualizarQrDesdeTransaccion(actualizada);

            actualizarEstadoCorreo();

        } catch (Exception e) {

            /*
             * No propagamos esta excepción hacia JSF.
             *
             * Un problema refrescando la pantalla
             * jamás debe afectar una reserva ya confirmada.
             */
            log.error("Error refrescando estado de reserva. requestId={}", this.requestId, e);
        }
    }

    // =========================================================
    // QR
    // =========================================================

    private void actualizarQrDesdeTransaccion(Transaccion transaccion) {

        this.qrImage = null;
        this.qrDisponible = false;

        if (!this.pagoExitoso || transaccion == null) {

            return;
        }

        byte[] codigoQR = transaccion.getTrxCodigoQR();

        if (codigoQR == null || codigoQR.length == 0) {

            return;
        }

        /*
         * Generamos StreamedContent utilizando los bytes
         * que YA están almacenados en PostgreSQL.
         */
        byte[] qrSeguro = codigoQR.clone();

        this.qrImage = DefaultStreamedContent.builder().contentType("image/png").stream(() -> new ByteArrayInputStream(qrSeguro)).build();

        this.qrDisponible = true;
    }

    // =========================================================
    // ESTADO DEL CORREO
    // =========================================================

    private void actualizarEstadoCorreo() {

        this.correoEnviado = false;
        this.correoError = false;
        this.correoPendiente = false;

        if (!this.pagoExitoso || this.transaccion == null) {

            return;
        }

        try {

            ProcesoPostPago proceso = procesoPostPagoService.obtener(this.transaccion.getTrxRequestId(), this.transaccion.getTrxReferencia());

            /*
             * Puede existir una ventana extremadamente
             * pequeña entre la confirmación y la creación
             * del proceso postpago.
             */
            if (proceso == null) {

                this.correoPendiente = true;

                return;
            }

            String estadoCorreo = proceso.getEstadoCorreo();

            if (EstadoProcesoPostPago.COMPLETADO.name().equalsIgnoreCase(estadoCorreo)) {

                this.correoEnviado = true;

                return;
            }

            if (EstadoProcesoPostPago.ERROR.name().equalsIgnoreCase(estadoCorreo)) {

                /*
                 * El correo pudo fallar.
                 *
                 * Esto NO significa que la reserva
                 * haya fallado.
                 */
                this.correoError = true;

                return;
            }

            this.correoPendiente = true;

        } catch (Exception e) {

            /*
             * Tampoco propagamos errores de consulta
             * del postproceso hacia la pantalla.
             */
            this.correoPendiente = true;

            log.warn("No fue posible consultar estado de correo. requestId={}", this.requestId, e);
        }
    }

    // =========================================================
    // CONTROL DEL POLLING
    // =========================================================

    /**
     * Por ahora detenemos el polling cuando tenemos QR.
     * <p>
     * No mantenemos al navegador consultando indefinidamente
     * esperando SMTP.
     */
    public boolean isPostPagoFinalizado() {

        return !this.pagoExitoso || this.qrDisponible;
    }

    // =========================================================
    // TEXTOS DE LA INTERFAZ
    // =========================================================

    public String getTituloResultado() {

        if (this.pagoExitoso) {
            return "¡Reserva exitosa!";
        }

        if (this.pagoRechazado) {
            return "Pago rechazado";
        }

        if (this.pagoPendiente) {
            return "Pago pendiente";
        }

        return "Estado de la transacción";
    }

    public String getSubtituloResultado() {

        if (this.pagoExitoso) {

            return "Tu pago fue procesado correctamente " + "y la parrilla ha sido reservada.";
        }

        if (this.message != null && !this.message.isBlank()) {

            return this.message;
        }

        return "Revise el detalle de la transacción.";
    }

    // =========================================================
    // FINALIZAR
    // =========================================================

    public String finalizarPago() {

        this.transaccion = new Transaccion();

        this.pagoResponseDTO = new PagoResponseDTO();

        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();

        sessionMap.remove("requestId");

        return "/booking?faces-redirect=true";
    }

    // =========================================================
    // REDIRECCIÓN
    // =========================================================

    private void redirigirABooking() {

        try {

            FacesContext.getCurrentInstance().getExternalContext().redirect("booking.xhtml");

        } catch (IOException e) {

            log.error("Error al redirigir a booking.xhtml. requestId={}", this.requestId, e);

            try {

                FacesContext.getCurrentInstance().getExternalContext().redirect("error.xhtml");

            } catch (IOException ex) {

                log.error("Error crítico: no se pudo redirigir " + "a la página de error. requestId={}", this.requestId, ex);
            }
        }
    }
}