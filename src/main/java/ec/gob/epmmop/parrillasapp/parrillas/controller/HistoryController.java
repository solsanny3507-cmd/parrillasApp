package ec.gob.epmmop.parrillasapp.parrillas.controller;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.CiudadanoService;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.service.MailService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Getter
@Setter
@Named
@ViewScoped
public class HistoryController extends Utilitarios implements Serializable {

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private CiudadanoService ciudadanoService;

    @Inject
    private MailService mailService;

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String PENDING  = "PENDING";
    private static final Logger LOGGER = Logger.getLogger(HistoryController.class.getName());

    private List<Transaccion> transaccionesList;
    private Ciudadano ciudadano;

    private String identificacion;
    private Integer token;

    private boolean verificarCliente;
    private boolean existeCliente;
    private boolean existenReservas;

    @PostConstruct
    public void init() {
        this.verificarCliente = true;
        this.existeCliente = false;
        this.existenReservas = false;
    }

    public void consultarPersona(){
        Optional<Ciudadano> personaOpt = this.ciudadanoService.buscarPersona(identificacion);

        if (personaOpt.isPresent()) {
            this.ciudadano = personaOpt.get();
            this.verificarCliente =false;
            this.existeCliente = true;
            this.existenReservas = false;

            this.ciudadanoService.modificarToken(this.ciudadano);

            String html = construirHtmlVerificacion(this.ciudadano);
            String asunto = "Verificación de código OTP";

            try {
                this.mailService.enviarHtml(this.ciudadano.getCiuCorreo(), asunto, html);
            } catch (Exception e) {
                LOGGER.warning("No se pudo enviar el correo: " + e.getMessage());
            }
        } else {
            PrimeFaces.current().executeScript(
                    "mostrarMensaje('warning','Usuario no registrado', 'No se encontraron usuarios con ese número de identificación')");
        }
    }

    public void validarToken(){

        if(this.token.equals(this.ciudadano.getUsuario().getUsxToken())){

            if(obtenerFechaActual().isBefore(this.ciudadano.getUsuario().getUsxTokenExpira())){
                this.transaccionesList = this.transaccionService.obtenerTransaccionesPorCliente(this.ciudadano);

                if (this.transaccionesList.isEmpty()) {
                    PrimeFaces.current().executeScript(
                            "mostrarMensaje('warning','Usuario no registrado', 'No se encontraron usuarios con ese número de identificación')");
                } else {

                    for(Transaccion transaccion : this.transaccionesList){
                        transaccion.setTrxEstadoTransaccion(transaccion.getTrxEstadoTransaccion());
                    }

                    this.verificarCliente =false;
                    this.existeCliente = false;
                    this.existenReservas = true;
                }

                this.ciudadanoService.modificarToken(this.ciudadano);

            } else{
                this.ciudadanoService.modificarToken(this.ciudadano);
                PrimeFaces.current().executeScript(
                        "mostrarMensaje('error','Token Expirado', 'El token ingresado ha expirado')");
            }


        } else {
            PrimeFaces.current().executeScript(
                    "mostrarMensaje('error','Token Incorrecto', 'El token ingresado no es válido')");
        }
    }


    public void nuevaConsulta(){
        this.identificacion = null;
        this.token = null;
        this.ciudadano = new Ciudadano();
        this.transaccionesList = new ArrayList<>();
        this.verificarCliente =true;
        this.existeCliente = false;
        this.existenReservas = false;
    }
}
