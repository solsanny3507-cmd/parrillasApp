package ec.gob.epmmop.parrillasapp.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ReservaActivaResponseDTO {

    private String identificacion;
    private int cantidad;
    private List<ReservaActivaDTO> reservas;

    public ReservaActivaResponseDTO(
            String identificacion,
            List<ReservaActivaDTO> reservas) {

        this.identificacion = identificacion;
        this.reservas = reservas;
        this.cantidad = reservas != null
                ? reservas.size()
                : 0;
    }

    public void setReservas(
            List<ReservaActivaDTO> reservas) {

        this.reservas = reservas;
        this.cantidad = reservas != null
                ? reservas.size()
                : 0;
    }
}