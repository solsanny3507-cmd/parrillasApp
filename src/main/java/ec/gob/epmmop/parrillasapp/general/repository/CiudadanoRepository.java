package ec.gob.epmmop.parrillasapp.general.repository;


import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.util.Optional;

@Repository
public interface CiudadanoRepository extends EntityRepository<Ciudadano, Integer> {

    @Query("SELECT e FROM Ciudadano e WHERE e.ciuEstado = TRUE AND e.ciuIdentificacion = :identificacion")
    Optional<Ciudadano> buscarPersonaPorIdentificacion(@QueryParam("identificacion") String identificacion);

}
