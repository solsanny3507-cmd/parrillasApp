package ec.gob.epmmop.parrillasapp.placetopay.dto;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    private Ciudadano ciudadano;
    private String correo;
    private String codigoServicio;
    private String nombreServicio;
    private BigDecimal precio;
    private String ip;
}
