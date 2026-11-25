package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ServicioRepository extends EntityRepository<Servicio, Integer> {

    @Query("SELECT e FROM Servicio e WHERE e.serCodigo = 'PAR' AND e.serEstado = TRUE")
    Servicio obtenerServicioParrilas();
}
