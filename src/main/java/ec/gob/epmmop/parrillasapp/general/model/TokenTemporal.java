package ec.gob.epmmop.parrillasapp.general.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "eusutoken", schema = "general")
public class TokenTemporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TKN_SECUENCIAL")
    private Integer tknSecuencial;

    @Column(name = "TKN_IDENTIFICACION")
    private String tknIdentificacion;

    @Column(name = "TKN_CORREO")
    private String tknCorreo;

    @Column(name = "TKN_TOKEN")
    private Integer tknToken;

    @Column(name = "TKN_FECHAREG")
    private LocalDateTime tknFechaRegistro;

    @Column(name = "TKN_FECHAEXP")
    private LocalDateTime tknFechaExpiracion;

    @Column(name = "TKN_ESTADO")
    private Boolean tknEstado;

}
