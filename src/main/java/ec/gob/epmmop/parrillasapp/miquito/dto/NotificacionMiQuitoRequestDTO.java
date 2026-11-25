package ec.gob.epmmop.parrillasapp.miquito.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionMiQuitoRequestDTO {

    private String identificacion;
    private String titulo;
    private String cuerpo;
    private String deeplink;
}