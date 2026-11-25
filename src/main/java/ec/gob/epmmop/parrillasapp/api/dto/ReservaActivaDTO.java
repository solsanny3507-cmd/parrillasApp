package ec.gob.epmmop.parrillasapp.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaActivaDTO {

//    private Integer reservaId;

    private String fechaReserva;

    private String estado;

    private String parque;

//    private String ubicacionParque;

    private String grupoParrilla;

//    private Integer parrillaId;

    private String parrilla;

//    private String detalleParrilla;

//    private Integer capacidad;

    private Integer requestId;

    private String referenciaPago;

    private BigDecimal monto;

    private String qrMimeType;

    private String qrBase64;
}