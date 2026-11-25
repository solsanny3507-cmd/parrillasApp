package ec.gob.epmmop.parrillasapp.parrillas.dto;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@ToString
public class DatosReservaDTO {

    private Parrilla parrilla;
    private Ciudadano ciudadano;
    private LocalDate fechaReserva;
    private BigDecimal precioTotal;
    private BigDecimal baseImponible;
    private BigDecimal valorIVA;
    private BigDecimal ivaPorcentaje;

}
