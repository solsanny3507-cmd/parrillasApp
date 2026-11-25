package ec.gob.epmmop.parrillasapp.placetopay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "eprocesopostpago", schema = "general", uniqueConstraints = {@UniqueConstraint(name = "uq_eprocesopostpago_pago", columnNames = {"ppp_request_id", "ppp_reference"})})
public class ProcesoPostPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ppp_secuencial")
    private Long secuencial;

    @Column(name = "ppp_request_id", nullable = false)
    private Integer requestId;

    @Column(name = "ppp_reference", nullable = false)
    private String reference;

    @Column(name = "ppp_status_ptp")
    private String statusPlaceToPay;

    @Column(name = "ppp_authorization")
    private String authorization;

    @Column(name = "ppp_nombre_tarjeta")
    private String nombreTarjeta;

    @Column(name = "ppp_reason")
    private String reason;

    @Column(name = "ppp_message")
    private String message;

    @Column(name = "ppp_fecha_pago")
    private String fechaPago;

    // ==========================================================
    // FACTURA
    // ==========================================================

    @Column(name = "ppp_estado_factura", nullable = false)
    private String estadoFactura;

    @Column(name = "ppp_intentos_factura", nullable = false)
    private Integer intentosFactura;

    @Column(name = "ppp_error_factura")
    private String errorFactura;

    @Column(name = "ppp_fecha_factura")
    private LocalDateTime fechaFactura;

    // ==========================================================
    // QR
    // ==========================================================

    @Column(name = "ppp_estado_qr", nullable = false)
    private String estadoQr;

    @Column(name = "ppp_intentos_qr", nullable = false)
    private Integer intentosQr;

    @Column(name = "ppp_error_qr")
    private String errorQr;

    @Column(name = "ppp_fecha_qr")
    private LocalDateTime fechaQr;

    // ==========================================================
    // CORREO
    // ==========================================================

    @Column(name = "ppp_estado_correo", nullable = false)
    private String estadoCorreo;

    @Column(name = "ppp_intentos_correo", nullable = false)
    private Integer intentosCorreo;

    @Column(name = "ppp_error_correo")
    private String errorCorreo;

    @Column(name = "ppp_fecha_correo")
    private LocalDateTime fechaCorreo;

    // ==========================================================
    // CONTROL DE PROCESAMIENTO
    // ==========================================================

    @Column(name = "ppp_en_proceso", nullable = false)
    private Boolean enProceso;

    @Column(name = "ppp_fecha_inicio_proceso")
    private LocalDateTime fechaInicioProceso;

    @Column(name = "ppp_fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "ppp_fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // ==========================================================
    // NOTIFICACIÓN MI QUITO
    // ==========================================================

    @Column(name = "ppp_estado_notificacion", nullable = false)
    private String estadoNotificacion;

    @Column(name = "ppp_intentos_notificacion", nullable = false)
    private Integer intentosNotificacion;

    @Column(name = "ppp_error_notificacion")
    private String errorNotificacion;

    @Column(name = "ppp_fecha_notificacion")
    private LocalDateTime fechaNotificacion;
}
