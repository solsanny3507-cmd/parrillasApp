package ec.gob.epmmop.parrillasapp.general.service;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Usuario;
import ec.gob.epmmop.parrillasapp.general.repository.CiudadanoRepository;
import ec.gob.epmmop.parrillasapp.general.repository.UsuarioRepository;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class CiudadanoService extends Utilitarios {

    @Inject
    private CiudadanoRepository ciudadanoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private UsuarioService usuarioService;

//    private Usuario usuario;

    @Transactional(rollbackOn = Exception.class)
    public Ciudadano registrar(Ciudadano ciudadano) {

        if (ciudadano.getCiuNombre() == null || ciudadano.getCiuNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del ciudadano es obligatorio");
        }

        if (ciudadano.getCiuCorreo() == null || ciudadano.getCiuCorreo().isBlank()) {
            throw new IllegalArgumentException("El correo del ciudadano es obligatorio");
        }

        if (ciudadano.getCiuTelefono() == null || ciudadano.getCiuTelefono().isBlank()) {
            throw new IllegalArgumentException("El teléfono del ciudadano es obligatorio");
        }

        ciudadano.setCiuNombre(capitalizarTexto(ciudadano.getCiuNombre().trim()));
        ciudadano.setCiuCorreo(ciudadano.getCiuCorreo().toLowerCase().trim());
        ciudadano.setCiuTelefono(ciudadano.getCiuTelefono().trim());
        ciudadano.setCiuEstado(Boolean.TRUE);
        ciudadano.setCiuFechaRegistro(obtenerFechaActual());
        ciudadano = ciudadanoRepository.save(ciudadano);


        Optional<Usuario> usuarioOpt = usuarioRepository.buscarUsuario(ciudadano.getCiuSecuencial());

        if (usuarioOpt.isEmpty()) {
            String cedula = ciudadano.getCiuIdentificacion();
            String hashedPassword = BCrypt.hashpw(cedula, BCrypt.gensalt());

            Usuario usuario = new Usuario();
            usuario.setCiudadano(ciudadano);
            usuario.setUsxClave(hashedPassword);
            usuario.setUsxRecupera(Boolean.FALSE);
            usuarioRepository.save(usuario);
        }

        return ciudadano;
    }

    public Optional<Ciudadano> buscarPersona(String identificacion) {
        return ciudadanoRepository.buscarPersonaPorIdentificacion(identificacion);
    }

    @Transactional(rollbackOn = Exception.class)
    public Ciudadano modificarToken(Ciudadano ciudadano) {
        usuarioService.modificarToken(ciudadano.getUsuario());
        return ciudadano;
    }
}
