package ec.gob.epmmop.parrillasapp.parrillas.repository;

import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ParrillaRepository extends EntityRepository<Parrilla, Integer> {

    @Query("""
                SELECT p
                FROM Parrilla p
                JOIN p.grupoParrilla gp
                JOIN gp.parque pa
                WHERE p.asaEstado = TRUE
                AND pa.parSecuencial = :idParque
                AND NOT EXISTS (
                    SELECT r
                    FROM ReservaParrilla r
                    WHERE r.parrilla = p
                    AND r.repFechaReserva = :fecha
                    AND r.repActivo = TRUE
                )
                ORDER BY p.grupoParrilla.graSecuencial, p.asaNombre
            """)
    List<Parrilla> listarParrillasDisponibles(@QueryParam("idParque") Integer idParque, @QueryParam("fecha") LocalDate fecha);

    @Query("SELECT p FROM Parrilla p WHERE p.asaSecuencial = :idParrilla AND p.asaEstado = TRUE")
    Parrilla obtenerParrilla(@QueryParam("idParrilla") Integer idParrilla);
}
