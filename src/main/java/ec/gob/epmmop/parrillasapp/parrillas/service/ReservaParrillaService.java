package ec.gob.epmmop.parrillasapp.parrillas.service;

import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.parrillas.exception.ReservaNoDisponibleException;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoReserva;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.repository.ReservaParrillaRepository;
import ec.gob.epmmop.parrillasapp.placetopay.config.PagoConstants;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class ReservaParrillaService extends Utilitarios {

    private static final Logger LOGGER = Logger.getLogger(ReservaParrillaService.class.getName());

    /*
     * Debe coincidir con la duración enviada
     * a PlaceToPay.
     */
//    private static final long MINUTOS_RETENCION = 15L;


    private static final String CONSTRAINT_RESERVA_ACTIVA = "uq_ereservparri_activa_parrilla_fecha";

    @Inject
    private ReservaParrillaRepository reservaParrillaRepository;

    @Inject
    private TransaccionService transaccionService;

    @PersistenceContext(unitName = "EpmmopPU")
    private EntityManager entityManager;

    // =========================================================
    // MODIFICAR ESTADO DE RESERVA POR RESPUESTA PLACE TO PAY
    // =========================================================

    @Transactional(rollbackOn = Exception.class)
    public ReservaParrilla modificarReserva(ReservaParrilla reservaParrilla, PagoResponseDTO pagoResponseDTO) {

        String statusRespuesta = transformarStatusReserva(pagoResponseDTO.getStatus());

        switch (statusRespuesta) {

            case APROBADO -> {

                reservaParrilla.setRepActivo(Boolean.TRUE);

                reservaParrilla.setRepEstadoPago(EstadoReserva.RESERVADA.getDescripcion());

                /*
                 * Ya no necesitamos una expiración
                 * temporal porque el pago terminó.
                 */
                reservaParrilla.setRepFechaExpiracion(null);
            }

            case RECHAZADO -> {

                reservaParrilla.setRepActivo(Boolean.FALSE);

                reservaParrilla.setRepEstadoPago(EstadoReserva.CANCELADA.getDescripcion());

                reservaParrilla.setRepFechaExpiracion(null);
            }

            case PENDIENTE -> {

                /*
                 * PENDING sigue bloqueando físicamente
                 * la parrilla.
                 */
                reservaParrilla.setRepActivo(Boolean.TRUE);

                reservaParrilla.setRepEstadoPago(EstadoReserva.PENDIENTE.getDescripcion());
            }

            default -> throw new IllegalArgumentException("Estado de pago no soportado: " + statusRespuesta);
        }

        return reservaParrillaRepository.save(reservaParrilla);
    }

    // =========================================================
    // RETENCIÓN TEMPORAL
    // =========================================================

    /**
     * La protección definitiva contra doble reserva
     * corresponde al índice único de PostgreSQL.
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public ReservaParrilla crearRetencion(DatosReservaDTO datosReservaDTO) {

        LocalDateTime ahora = obtenerFechaActual();

        ReservaParrilla reserva = new ReservaParrilla();

        reserva.setParrilla(datosReservaDTO.getParrilla());

        reserva.setCiudadano(datosReservaDTO.getCiudadano());

        reserva.setRepFechaReserva(datosReservaDTO.getFechaReserva());

        reserva.setRepFechaRegistro(ahora);

        reserva.setRepFechaExpiracion(ahora.plusMinutes(PagoConstants.DURACION_SESION_MINUTOS));

        reserva.setRepActivo(Boolean.TRUE);

        reserva.setRepEstadoPago(EstadoReserva.PENDIENTE.getDescripcion());

        reserva.setRepObservacion("Retención temporal para proceso de pago");

        try {

            reserva = reservaParrillaRepository.save(reserva);

            /*
             * Fundamental:
             *
             * obligamos a PostgreSQL a evaluar el índice
             * único AQUÍ y no cuando el interceptor haga
             * commit al salir del método.
             */
            entityManager.flush();

            LOGGER.info("Retención creada" + " | reserva=" + reserva.getRepSecuencial() + " | parrilla=" + reserva.getParrilla().getAsaSecuencial() + " | fecha=" + reserva.getRepFechaReserva() + " | expira=" + reserva.getRepFechaExpiracion());

            return reserva;

        } catch (PersistenceException e) {

            if (esConflictoReservaActiva(e)) {

                throw new ReservaNoDisponibleException("La parrilla acaba de ser reservada " + "temporalmente por otro usuario.", e);
            }

            throw e;
        }
    }

    // =========================================================
    // REGISTRAR TRANSACCIÓN SOBRE RESERVA YA RETENIDA
    // =========================================================

    @Transactional(rollbackOn = Exception.class)
    public ReservaParrilla registrarTransaccionReservaExistente(ReservaParrilla reserva, Servicio servicio, PlacetoPayResponse placetoPayResponse, DatosReservaDTO datosReservaDTO) {

        if (reserva == null || reserva.getRepSecuencial() == null) {

            throw new IllegalArgumentException("La reserva temporal no existe");
        }

        ReservaParrilla reservaActual = reservaParrillaRepository.findByRepSecuencial(reserva.getRepSecuencial());

        if (reservaActual == null) {

            throw new IllegalStateException("No existe la reserva temporal id=" + reserva.getRepSecuencial());
        }

        if (!Boolean.TRUE.equals(reservaActual.getRepActivo())) {

            throw new IllegalStateException("La reserva temporal ya no está activa");
        }

        if (!EstadoReserva.PENDIENTE.getDescripcion().equalsIgnoreCase(reservaActual.getRepEstadoPago())) {

            throw new IllegalStateException("La reserva ya no se encuentra PENDIENTE");
        }

        /*
         * Reserva + transacción quedan relacionadas.
         *
         * TransaccionService.registrar() debe usar
         * transacción REQUIRED/default, NO REQUIRES_NEW.
         */
        transaccionService.registrar(reservaActual, servicio, placetoPayResponse, datosReservaDTO);

        return reservaActual;
    }

    // =========================================================
    // LIBERAR RETENCIÓN
    // =========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public void liberarRetencion(Integer idReserva, String observacion) {

        if (idReserva == null) {
            return;
        }

        ReservaParrilla reserva = reservaParrillaRepository.findByRepSecuencial(idReserva);

        if (reserva == null) {
            return;
        }

        /*
         * Jamás liberamos una reserva APROBADA.
         */
        if (!EstadoReserva.PENDIENTE.getDescripcion().equalsIgnoreCase(reserva.getRepEstadoPago())) {

            return;
        }

        reserva.setRepActivo(Boolean.FALSE);

        reserva.setRepEstadoPago(EstadoReserva.CANCELADA.getDescripcion());

        reserva.setRepFechaExpiracion(null);

        reserva.setRepObservacion(limitarObservacion(observacion));

        reservaParrillaRepository.save(reserva);

        LOGGER.info("Retención liberada" + " | reserva=" + idReserva);
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    public ReservaParrilla obtenerReserva(Integer idReserva) {

        return reservaParrillaRepository.findByRepSecuencial(idReserva);
    }

    public Optional<ReservaParrilla> buscarReservaActiva(Parrilla parrilla, LocalDate fechaReserva) {

        return reservaParrillaRepository.buscarReservaActiva(parrilla.getAsaSecuencial(), fechaReserva);
    }

    public List<ReservaParrilla> obtenerRetencionesPagoVencidas() {

        return reservaParrillaRepository.obtenerRetencionesPagoVencidas(EstadoReserva.PENDIENTE.getDescripcion(), LocalDateTime.now());
    }

    public List<ReservaParrilla> obtenerReservasExpiradas() {

        return reservaParrillaRepository.obtenerReservasExpiradas(EstadoReserva.RESERVADA.getDescripcion());
    }

    // =========================================================
    // RESERVA YA UTILIZADA
    // =========================================================

    @Transactional(rollbackOn = Exception.class)
    public ReservaParrilla modificarEstadoExpirado(ReservaParrilla reservaParrilla) {

        reservaParrilla.setRepActivo(Boolean.FALSE);

        reservaParrilla.setRepEstadoPago(EstadoReserva.EXPIRADA.getDescripcion());

        return reservaParrillaRepository.save(reservaParrilla);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean esConflictoReservaActiva(Throwable error) {

        Throwable actual = error;

        while (actual != null) {

            /*
             * Primera protección:
             *
             * Buscamos directamente el nombre del índice/constraint
             * que creamos para impedir dos reservas activas.
             */
            String mensaje = actual.getMessage();

            if (mensaje != null && mensaje.contains(CONSTRAINT_RESERVA_ACTIVA)) {

                return true;
            }

            /*
             * Segunda protección:
             *
             * PostgreSQL utiliza SQLState 23505 para:
             *
             * unique_violation
             *
             * Esto evita depender de clases específicas
             * de Hibernate.
             */
            if (actual instanceof SQLException sqlException) {

                if ("23505".equals(sqlException.getSQLState())) {

                    return true;
                }
            }

            actual = actual.getCause();
        }

        return false;
    }

    private String limitarObservacion(String observacion) {

        if (observacion == null) {
            return null;
        }

        return observacion.length() <= 100 ? observacion : observacion.substring(0, 100);
    }
}
