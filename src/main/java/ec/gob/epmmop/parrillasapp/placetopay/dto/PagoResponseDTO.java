package ec.gob.epmmop.parrillasapp.placetopay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagoResponseDTO {

    private String status;
    private String reason;
    private String message;
    private String date;
    private String reference;
    private String authorization;
    private String nombreTarjeta;
}
