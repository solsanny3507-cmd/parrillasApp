package ec.gob.epmmop.parrillasapp.general.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "euserextapp", schema = "general")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USX_SECUENCIAL")
    private Integer usxSecuencial;

    @Column(name = "USX_NICKNAME", nullable = false)
    private String usxNickname;

    @Column(name = "USX_CLAVE", nullable = false)
    private String usxClave;

    @Column(name = "USX_RECUPERA")
    private Boolean usxRecupera;

    @Column(name = "USX_TOKEN")
    private Integer usxToken;

    @Column(name = "USX_TOKEN_CREA")
    private LocalDateTime usxTokenCrea;

    @Column(name = "USX_TOKEN_EXPIRA")
    private LocalDateTime usxTokenExpira;

    // Relación 1:1 con Ciudadano
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ciu_secuencial", nullable = false, unique = true)
    private Ciudadano ciudadano;
}
