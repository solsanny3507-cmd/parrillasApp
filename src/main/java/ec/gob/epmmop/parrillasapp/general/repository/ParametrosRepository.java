package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.model.Parametros;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ParametrosRepository extends EntityRepository<Parametros, Integer> {

    Parametros findByPrmSecuencial(Integer prmSecuencial);
}
