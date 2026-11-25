package ec.gob.epmmop.parrillasapp.parrillas.controller;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.CiudadanoService;
import ec.gob.epmmop.parrillasapp.general.service.ServicioService;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.parrillas.exception.ReservaNoDisponibleException;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parque;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.service.BookingService;
import ec.gob.epmmop.parrillasapp.parrillas.service.ParqueService;
import ec.gob.epmmop.parrillasapp.parrillas.service.ParrillaService;
import ec.gob.epmmop.parrillasapp.parrillas.service.ReservaParrillaService;
import ec.gob.epmmop.parrillasapp.placetopay.config.PagoException;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayResponse;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Named
@ViewScoped
public class BookingController extends Utilitarios implements Serializable {

    @Inject
    private ParqueService parqueService;

    @Inject
    private ParrillaService parrillaService;

    @Inject
    private ServicioService servicioService;

    @Inject
    private CiudadanoService ciudadanoService;

    @Inject
    private ReservaParrillaService reservaParrillaService;

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private BookingService bookingService;

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    private static final String STATUS_OK = "OK";

    private Parque parque;
    private List<Parque> parquesList;
    private Parrilla parrilla;
    private List<Parrilla> parrillasDisponiblesList;
    private Servicio servicio;
    private Ciudadano ciudadano;
    private ReservaParrilla reservaParrilla;
    private ReservaParrilla reservaParrillaPendiente;
    private Transaccion transaccion;
    private Transaccion transaccionPendiente;

    private DatosReservaDTO datosReservaDTO;
    private PlacetoPayResponse placetoPayResponse;

    // ====== Auxiliares ======
    private LocalDate fechaMinima = LocalDate.now();
    private LocalDate fechaMaxima = LocalDate.now().plusDays(30);
    private LocalDate fechaReserva;
    private boolean existeDisponibilidad;
    private String identificacion;
    private BigDecimal precioTotal;
    private BigDecimal baseImponible;
    private BigDecimal valorIVA;
    private BigDecimal ivaPorcentaje;

    // ====== Control de vista ======
    private boolean booking;
    private boolean aceptaReserva;
    private boolean aceptaTerminos;
    private boolean aceptaPoliticas;
    private boolean habilitarPago;

    @PostConstruct
    public void init() {
        this.ciudadano = new Ciudadano();
        this.cargarParquesBBQ();
        this.booking = true;
        this.aceptaTerminos = false;
        this.aceptaPoliticas = false;
        this.habilitarPago = true;
    }

    public void cargarParquesBBQ() {
        this.parquesList = this.parqueService.listarParquesBBQ();
        this.servicio = this.servicioService.obtenerServicioParrilas();

    }

    public void cargarParrillasDisponibles() {
        this.parrillasDisponiblesList = this.parrillaService.listarParrillasDisponibles(this.parque, this.fechaReserva);

        this.existeDisponibilidad = !parrillasDisponiblesList.isEmpty();

        if (!this.existeDisponibilidad) {
            PrimeFaces.current().executeScript("mostrarMensaje('warning','No hay disponibilidad', 'Seleccione otra fecha de reserva')");
        }
    }

    public void verDetalle(Parrilla parrilla) {
        this.parrilla = parrilla;
        if (this.parrilla.getAsaSecuencial() != null) {
            this.booking = false;
            this.calcularValores(this.servicio.getSerPrecio());
        }
    }

    public void calcularValores(BigDecimal precioServicio) {
        this.precioTotal = precioServicio;
        this.ivaPorcentaje = new BigDecimal(this.servicio.getSerIva());
        this.baseImponible = calcularBaseImponible(this.precioTotal, this.ivaPorcentaje);
        this.valorIVA = calcularIva(this.precioTotal, this.ivaPorcentaje);
    }

    public void regresarBooking() {
        this.parrilla = new Parrilla();
        this.booking = true;
        this.aceptaTerminos = false;
        this.aceptaPoliticas = false;
        this.habilitarPago = true;
    }

    public void verificarCliente() {
        Optional<Ciudadano> personaOpt = this.ciudadanoService.buscarPersona(this.identificacion);

        if (personaOpt.isPresent()) {
            this.ciudadano = personaOpt.get();
        } else {
            this.ciudadano = new Ciudadano();
            this.ciudadano.setCiuIdentificacion(this.identificacion);
            this.ciudadano.setCiuNombre(obtenerNombrePorCedula(this.identificacion));
            this.ciudadano.setCiuCorreo(null);
            this.ciudadano.setCiuTelefono(null);
        }
    }

    public void verificarAceptacion() {
        this.habilitarPago = !(this.aceptaPoliticas && this.aceptaTerminos && this.aceptaReserva);
    }

    public void validarPrevioPago() throws UnknownHostException {
        if (!validacionesPrevias(this.ciudadano, this.parrilla, fechaReserva)) return;
        this.procesarPago();
    }

    public boolean validacionesPrevias(Ciudadano ciudadano, Parrilla parrilla, LocalDate fechaReserva) {
        return this.validarDatosCliente(ciudadano) && this.validarDoblePago(ciudadano) && this.validarReservaPendienteDePago(parrilla, fechaReserva);
    }

    public void procesarPago() {

        ReservaParrilla retencion = null;

        boolean sesionPlaceToPayCreada = false;

        try {

            asegurarCiudadanoPersistido();

            crearDatosReserva();

            /*
             * PASO 1
             *
             * Intentamos ganar la parrilla ANTES
             * de llamar a PlaceToPay.
             */
            retencion = reservaParrillaService.crearRetencion(datosReservaDTO);

            this.reservaParrilla = retencion;

            /*
             * PASO 2
             *
             * Solo el usuario que obtuvo la retención
             * llega a crear una sesión PlaceToPay.
             */
            this.placetoPayResponse = bookingService.generarPago(this.ciudadano, this.parrilla, datosReservaDTO);

            if (!esRespuestaOK()) {

                reservaParrillaService.liberarRetencion(retencion.getRepSecuencial(), "PlaceToPay no creó la sesión de pago");

                PrimeFaces.current().executeScript("mostrarMensaje(" + "'error'," + "'Error'," + "'No fue posible iniciar el proceso de pago.')");

                return;
            }

            /*
             * Desde este punto sabemos que existe una
             * sesión de PlaceToPay.
             */
            sesionPlaceToPayCreada = true;

            /*
             * PASO 3
             *
             * Ya NO creamos otra ReservaParrilla.
             * Registramos la Transaccion utilizando
             * la retención existente.
             */
            this.reservaParrilla = reservaParrillaService.registrarTransaccionReservaExistente(retencion, this.servicio, this.placetoPayResponse, this.datosReservaDTO);

            /*
             * PASO 4
             */
            redirigirPago();

        } catch (ReservaNoDisponibleException e) {

            log.info("Parrilla tomada por otro usuario. parrilla={}, fecha={}", this.parrilla != null ? this.parrilla.getAsaSecuencial() : null, this.fechaReserva);

            PrimeFaces.current().executeScript("mostrarMensaje(" + "'warning'," + "'Parrilla no disponible'," + "'La parrilla acaba de ser reservada " + "temporalmente por otro usuario. " + "Seleccione otra opción o inténtelo " + "nuevamente más tarde.')");

            cargarParrillasDisponibles();

        } catch (PagoException e) {

            /*
             * Si PlaceToPay NO llegó a crear una sesión,
             * podemos liberar inmediatamente la parrilla.
             */
            if (retencion != null && !sesionPlaceToPayCreada) {

                liberarRetencionSegura(retencion, "Error creando sesión PlaceToPay");
            }

            String mensaje = e.getMessage() != null ? e.getMessage() : "Error desconocido";

            mensaje = mensaje.replace("'", "\\'");

            PrimeFaces.current().executeScript("mostrarMensaje(" + "'error'," + "'Error'," + "'Error al crear sesión: " + mensaje + "')");

        } catch (Exception e) {

            /*
             * Si ya existe una sesión PlaceToPay,
             * NO liberamos automáticamente la parrilla.
             *
             * Es más seguro mantenerla bloqueada y
             * reconciliar que liberar un recurso cuya
             * sesión de pago ya existe.
             */
            if (retencion != null && !sesionPlaceToPayCreada) {

                liberarRetencionSegura(retencion, "Error antes de crear sesión PlaceToPay");
            }

            log.error("Error inesperado iniciando pago", e);

            PrimeFaces.current().executeScript("mostrarMensaje(" + "'error'," + "'No fue posible continuar'," + "'Se produjo un inconveniente al iniciar " + "el proceso de pago. Intente nuevamente.')");
        }
    }

    private void liberarRetencionSegura(ReservaParrilla reserva, String observacion) {

        try {

            if (reserva != null && reserva.getRepSecuencial() != null) {

                reservaParrillaService.liberarRetencion(reserva.getRepSecuencial(), observacion);
            }

        } catch (Exception e) {

            log.error("No fue posible liberar la retención {}", reserva != null ? reserva.getRepSecuencial() : null, e);
        }
    }

    public boolean esRespuestaOK() {
        return this.placetoPayResponse != null && this.placetoPayResponse.getStatus() != null && STATUS_OK.equalsIgnoreCase(this.placetoPayResponse.getStatus().getStatus());
    }

    public void redirigirPago() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("requestId", this.placetoPayResponse.getRequestId());
            FacesContext.getCurrentInstance().getExternalContext().redirect(this.placetoPayResponse.getProcessUrl());

        } catch (IOException e) {
            log.error("Error al redirigir a PlaceToPay", e);
            String mensajeError = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            mensajeError = mensajeError.replace("'", "\\'");
            PrimeFaces.current().executeScript("mostrarMensaje('error','Error','No se pudo redirigir al pago: " + mensajeError + "')");
        }
    }

    public void crearDatosReserva() {
        this.datosReservaDTO = new DatosReservaDTO();
        this.datosReservaDTO.setCiudadano(this.ciudadano);
        this.datosReservaDTO.setParrilla(this.parrilla);
        this.datosReservaDTO.setFechaReserva(this.fechaReserva);
        this.datosReservaDTO.setPrecioTotal(this.precioTotal);
        this.datosReservaDTO.setValorIVA(this.valorIVA);
        this.datosReservaDTO.setIvaPorcentaje(this.ivaPorcentaje);
        this.datosReservaDTO.setBaseImponible(this.baseImponible);
    }

    public boolean validarDatosCliente(Ciudadano ciudadano) {
        if (!esCorreoValido(ciudadano.getCiuCorreo())) {
            PrimeFaces.current().executeScript("mostrarMensaje('warning','Correo No Válido', 'El correo ingresado no tiene un formato válido')");
            return false;
        }
        return true;
    }

    public boolean validarDoblePago(Ciudadano ciudadano) {

        /*
         * Ciudadano nuevo.
         *
         * Todavía no existe en la BD y por lo tanto
         * no puede tener una transacción pendiente
         * asociada mediante ciu_secuencial.
         */
        if (ciudadano == null || ciudadano.getCiuSecuencial() == null) {

            return true;
        }

        this.transaccionPendiente = new Transaccion();

        Optional<Transaccion> optTransaccion = this.transaccionService.buscarTransaccionPendiente(ciudadano);

        if (optTransaccion.isPresent()) {

            this.transaccionPendiente = optTransaccion.get();

            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("requestId", this.transaccionPendiente.getTrxRequestId());

            String url = this.transaccionPendiente.getTrxUrl().replace("'", "\\'");

            String ref = this.transaccionPendiente.getTrxReferencia().replace("'", "\\'");

            PrimeFaces.current().executeScript("mostrarConfirmacion('" + ref + "', '" + url + "')");

            return false;
        }

        return true;
    }

    public boolean validarReservaPendienteDePago(Parrilla parrilla, LocalDate fechaReserva) {

        this.reservaParrillaPendiente = new ReservaParrilla();

        Optional<ReservaParrilla> optReserva = reservaParrillaService.buscarReservaActiva(parrilla, fechaReserva);

        if (optReserva.isPresent()) {

            this.reservaParrillaPendiente = optReserva.get();

            String nombreParrilla = this.reservaParrillaPendiente.getParrilla().getAsaNombre();

            String nombreParque = this.reservaParrillaPendiente.getParrilla().getGrupoParrilla().getParque().getParNombre();

            PrimeFaces.current().executeScript("mostrarMensaje(" + "'warning'," + "'No disponible'," + "'La " + nombreParrilla + " del Parque " + nombreParque + " ya se encuentra reservada " + "o en proceso de pago.')");

            return false;
        }

        return true;
    }

    private void asegurarCiudadanoPersistido() {

        if (this.ciudadano == null) {

            throw new IllegalStateException("No existe ciudadano asociado al proceso de reserva");
        }

        /*
         * Si ya tiene PK significa que fue recuperado de la BD.
         * No hacemos nada.
         */
        if (this.ciudadano.getCiuSecuencial() != null) {

            return;
        }

        /*
         * Ciudadano nuevo:
         * lo persistimos ANTES de crear la retención.
         *
         * Es fundamental conservar la instancia retornada por
         * CiudadanoService, porque ya contiene ciuSecuencial.
         */
        this.ciudadano = this.ciudadanoService.registrar(this.ciudadano);

        if (this.ciudadano.getCiuSecuencial() == null) {

            throw new IllegalStateException("No fue posible registrar al ciudadano");
        }
    }

}
