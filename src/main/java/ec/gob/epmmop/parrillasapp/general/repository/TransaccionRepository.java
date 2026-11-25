package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.util.List;

@Repository
public interface TransaccionRepository extends EntityRepository<Transaccion, Integer> {

    @Query("SELECT e FROM Transaccion e WHERE e.trxRequestId = :requestId")
    Transaccion obtenerTransaccionPorRequestId(@QueryParam("requestId") Integer requestId);

    @Query("""
        SELECT e
        FROM Transaccion e
        WHERE e.trxRequestId = :requestId
          AND e.trxReferencia = :reference
        """)
    Transaccion obtenerTransaccionPorRequestIdAndReference(
            @QueryParam("requestId") Integer requestId,
            @QueryParam("reference") String reference
    );

    @Query("SELECT e FROM Transaccion e WHERE e.ciudadano.ciuSecuencial = :idCliente AND LOWER(e.trxEstadoTransaccion) = 'pendiente' ORDER BY e.trxSecuencial DESC")
    List<Transaccion> buscarTransaccionPendiente(@QueryParam("idCliente") Integer idCliente);

    @Query("SELECT e FROM Transaccion e WHERE e.ciudadano.ciuSecuencial = :idCliente ORDER BY e.trxSecuencial DESC")
    List<Transaccion> buscarTransaccionPorCliente(@QueryParam("idCliente") Integer idCliente);

    @Query("SELECT e FROM Transaccion e WHERE LOWER(e.trxEstadoTransaccion) = 'pendiente' ORDER BY e.trxSecuencial")
    List<Transaccion> obtenerTransaccionesPendientes();

    @Query("""
        SELECT DISTINCT t
        FROM Transaccion t
        JOIN FETCH t.reservaParrilla
        JOIN FETCH t.reservaParrilla.parrilla
        JOIN FETCH t.reservaParrilla.parrilla.grupoParrilla
        JOIN FETCH t.reservaParrilla.parrilla.grupoParrilla.parque
        JOIN FETCH t.ciudadano
        LEFT JOIN FETCH t.servicio
        WHERE t.trxRequestId = :requestId
          AND t.trxReferencia = :reference
        """)
    Transaccion obtenerTransaccionCompleta(
            @QueryParam("requestId") Integer requestId,
            @QueryParam("reference") String reference
    );

    @Query("""
        SELECT t
        FROM Transaccion t
        WHERE t.reservaParrilla.repSecuencial = :idReserva
        """)
    List<Transaccion> buscarPorReserva(
            @QueryParam("idReserva") Integer idReserva
    );

    @Query("""
        SELECT DISTINCT t
        FROM Transaccion t
        JOIN FETCH t.reservaParrilla
        JOIN FETCH t.reservaParrilla.parrilla
        JOIN FETCH t.reservaParrilla.parrilla.grupoParrilla
        JOIN FETCH t.reservaParrilla.parrilla.grupoParrilla.parque
        JOIN FETCH t.ciudadano
        WHERE t.ciudadano.ciuIdentificacion = :identificacion
          AND t.ciudadano.ciuEstado = TRUE
          AND t.trxEstadoTransaccion = :estadoTransaccion
          AND t.trxCodigoQR IS NOT NULL
          AND t.reservaParrilla.repActivo = TRUE
          AND t.reservaParrilla.repEstadoPago = :estadoReserva
          AND t.reservaParrilla.repFechaReserva >= CURRENT_DATE
        ORDER BY t.reservaParrilla.repFechaReserva ASC,
                 t.trxSecuencial DESC
        """)
    List<Transaccion> buscarReservasActivasPorIdentificacion(
            @QueryParam("identificacion") String identificacion,
            @QueryParam("estadoTransaccion") String estadoTransaccion,
            @QueryParam("estadoReserva") String estadoReserva
    );

}
