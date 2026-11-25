package ec.gob.epmmop.parrillasapp.parrillas.repository;

import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaParrillaRepository
        extends EntityRepository<ReservaParrilla, Integer> {

    ReservaParrilla findByRepSecuencial(
            Integer idReserva
    );

    /**
     * Cualquier reserva activa bloquea la parrilla,
     * independientemente de si está PENDIENTE o RESERVADA.
     */
    @Query("""
            SELECT r
            FROM ReservaParrilla r
            WHERE r.parrilla.asaSecuencial = :idParrilla
              AND r.repFechaReserva = :fechaReserva
              AND r.repActivo = TRUE
            """)
    Optional<ReservaParrilla> buscarReservaActiva(
            @QueryParam("idParrilla") Integer idParrilla,
            @QueryParam("fechaReserva") LocalDate fechaReserva
    );

    /**
     * Retenciones de pago cuyo tiempo local ya terminó.
     *
     * IMPORTANTE:
     * encontrarlas aquí NO significa liberarlas.
     * Primero debemos consultar PlaceToPay.
     */
    @Query("""
            SELECT r
            FROM ReservaParrilla r
            WHERE r.repActivo = TRUE
              AND r.repEstadoPago = :estadoPendiente
              AND r.repFechaExpiracion IS NOT NULL
              AND r.repFechaExpiracion <= :ahora
            ORDER BY r.repFechaExpiracion
            """)
    List<ReservaParrilla> obtenerRetencionesPagoVencidas(
            @QueryParam("estadoPendiente") String estadoPendiente,
            @QueryParam("ahora") LocalDateTime ahora
    );

    /**
     * Reservas confirmadas cuya fecha de uso ya pasó.
     */
    @Query("""
            SELECT r
            FROM ReservaParrilla r
            WHERE r.repActivo = TRUE
              AND r.repEstadoPago = :estadoReservada
              AND r.repFechaReserva < CURRENT_DATE
            """)
    List<ReservaParrilla> obtenerReservasExpiradas(
            @QueryParam("estadoReservada") String estadoReservada
    );
}
