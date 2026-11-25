package ec.gob.epmmop.parrillasapp.placetopay.dto;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResultadoProcesamientoPagoDTO {

    private Transaccion transaccion;

    private PagoResponseDTO pagoResponse;

    private boolean actualizado;
}
