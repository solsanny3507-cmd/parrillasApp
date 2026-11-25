package ec.gob.epmmop.parrillasapp.general.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "eciudadano", schema = "general")
public class Ciudadano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CIU_SECUENCIAL")
    private Integer ciuSecuencial;

    @Column(name = "CIU_IDENTIFICACION")
    private String ciuIdentificacion;

    @Column(name = "CIU_NOMBRE")
    private String ciuNombre;

    @Column(name = "CIU_CORREO")
    private String ciuCorreo;

    @Column(name = "CIU_TELEFONO")
    private String ciuTelefono;

    @Column(name = "CIU_FECHAREG")
    private LocalDateTime ciuFechaRegistro;

    @Column(name = "CIU_ESTADO")
    private Boolean ciuEstado;

    // Relación 1:1 con Usuario
    @OneToOne(mappedBy = "ciudadano", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Usuario usuario;
}
