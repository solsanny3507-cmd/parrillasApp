package ec.gob.epmmop.parrillasapp.parrillas.repository;

import ec.gob.epmmop.parrillasapp.parrillas.model.Parque;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ParqueRepository extends EntityRepository<Parque, Integer> {

}
