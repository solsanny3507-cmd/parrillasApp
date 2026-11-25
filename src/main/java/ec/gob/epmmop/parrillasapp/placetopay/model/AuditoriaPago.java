package ec.gob.epmmop.parrillasapp.placetopay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "eauditoriapago", schema = "parrillas")
public class AuditoriaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aud_secuencial")
    private Long secuencial;

    @Column(name = "aud_request_id")
    private Integer requestId;

    @Column(name = "aud_reference", length = 150)
    private String reference;

    @Column(name = "aud_origen", nullable = false, length = 30)
    private String origen;

    @Column(name = "aud_evento", nullable = false, length = 50)
    private String evento;

    @Column(name = "aud_estado_ptp", length = 30)
    private String estadoPlaceToPay;

    @Column(name = "aud_estado_anterior", length = 30)
    private String estadoAnterior;

    @Column(name = "aud_estado_nuevo", length = 30)
    private String estadoNuevo;

    @Column(name = "aud_resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "aud_mensaje", length = 1000)
    private String mensaje;

    @Column(name = "aud_error", length = 2000)
    private String error;

    @Column(name = "aud_fecha", nullable = false)
    private LocalDateTime fecha;
}
