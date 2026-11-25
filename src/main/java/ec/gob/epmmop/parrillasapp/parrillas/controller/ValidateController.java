package ec.gob.epmmop.parrillasapp.parrillas.controller;

import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.service.QRCodeService;
import ec.gob.epmmop.parrillasapp.parrillas.service.ReservaParrillaService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Named
@RequestScoped
public class ValidateController implements Serializable {

    @Inject
    private ReservaParrillaService reservaParrillaService;

    @Inject
    private QRCodeService qrCodeService;

    private ReservaParrilla reserva;
    private String estadoClase;   // CSS: "activa" | "inactiva" | "invalida"
    private String estadoTexto;
    private String estadoIcono;
    private boolean tokenValido;

    @PostConstruct
    public void init() {
        ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
        String idParam    = ctx.getRequestParameterMap().get("id");
        String tokenParam = ctx.getRequestParameterMap().get("token");

        if (idParam == null || tokenParam == null) {
            marcarInvalida("Enlace de validación incorrecto.");
            return;
        }

        try {
            Integer idReserva = Integer.valueOf(idParam);
            this.reserva = reservaParrillaService.obtenerReserva(idReserva);

            if (this.reserva == null) {
                marcarInvalida("La reserva no existe.");
                return;
            }

            // Verificar que el token coincide con la reserva
            String tokenEsperado = qrCodeService.generarToken(
                    idReserva, "999999"
            );

            if (!tokenEsperado.equals(tokenParam)) {
                marcarInvalida("El código QR no es válido para esta reserva.");
                return;
            }

            // Verificar si la fecha de reserva ya pasó
            LocalDate fechaReserva = this.reserva.getRepFechaReserva();
            LocalDate hoy = LocalDate.now();

            if (fechaReserva.isBefore(hoy)) {
                this.tokenValido  = false;
                this.estadoClase  = "vencida";
                this.estadoTexto  = "Reserva Vencida";
                this.estadoIcono  = "⚠️";
            } else {
                this.tokenValido  = true;
                this.estadoClase  = "activa";
                this.estadoTexto  = "Reserva Válida y Activa";
                this.estadoIcono  = "✅";
            }

        } catch (NumberFormatException e) {
            marcarInvalida("Parámetros inválidos.");
        }
    }

    private void marcarInvalida(String motivo) {
        this.tokenValido = false;
        this.estadoClase = "invalida";
        this.estadoTexto = "Reserva No Válida — " + motivo;
        this.estadoIcono = "❌";
    }
}
