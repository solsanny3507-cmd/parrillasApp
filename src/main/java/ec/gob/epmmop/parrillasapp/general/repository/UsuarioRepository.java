package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.model.Usuario;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends EntityRepository<Usuario, Integer> {

    @Query("SELECT e FROM Usuario e WHERE e.ciudadano.ciuSecuencial = :idPersona")
    Optional<Usuario> buscarUsuario(@QueryParam("idPersona") Integer idPersona);

}
