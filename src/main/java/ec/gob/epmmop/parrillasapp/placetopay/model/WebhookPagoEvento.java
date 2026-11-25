package ec.gob.epmmop.parrillasapp.placetopay.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "ewebhookptp", schema = "general")
public class WebhookPagoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "whp_secuencial")
    private Long secuencial;

    @Column(name = "whp_request_id")
    private Integer requestId;

    @Column(name = "whp_reference")
    private String reference;

    @Column(name = "whp_status")
    private String status;

    @Column(name = "whp_status_date")
    private OffsetDateTime statusDate;

    @Column(name = "whp_estado")
    private String estado;

    @Column(name = "whp_intentos")
    private Integer intentos;

    @Column(name = "whp_ultimo_error")
    private String ultimoError;

    @Column(name = "whp_fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "whp_fecha_proceso")
    private LocalDateTime fechaProceso;
}
