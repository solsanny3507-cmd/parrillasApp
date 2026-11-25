package ec.gob.epmmop.parrillasapp.general.service;

import ec.gob.epmmop.parrillasapp.general.model.Usuario;
import ec.gob.epmmop.parrillasapp.general.repository.UsuarioRepository;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UsuarioService extends Utilitarios {

    @Inject
    private UsuarioRepository usuarioRepository;

    @Transactional(rollbackOn = Exception.class)
    public Usuario modificarToken(Usuario usuario) {

        if(usuario.getUsxToken() == null && usuario.getUsxTokenCrea() == null && usuario.getUsxTokenExpira() == null){
            usuario.setUsxToken(generarTokenNumerico());
            usuario.setUsxTokenCrea(obtenerFechaActual());
            usuario.setUsxTokenExpira(obtenerFechaActual().plusMinutes(5));
        }else{
            usuario.setUsxToken(null);
            usuario.setUsxTokenCrea(null);
            usuario.setUsxTokenExpira(null);
        }

        usuarioRepository.save(usuario);

        return usuario;
    }
}
