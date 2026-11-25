package ec.gob.epmmop.parrillasapp.general.model;

import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "etransaptp", schema = "general")
public class Transaccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "trx_secuencial")
	private Integer trxSecuencial;
	
	@Column(name = "trx_request_id")
	private Integer trxRequestId;
	
	@Column(name = "trx_transaccion_id")
	private String trxTransaccionId;
	
	@Column(name = "trx_referencia")
	private String trxReferencia;

	@Column(name = "trx_estado_trans")
	private String trxEstadoTransaccion;
	
	@Column(name = "trx_url")
	private String trxUrl;
	
	@Column(name = "trx_monto")
	private BigDecimal trxMonto;

	@Column(name = "trx_baseimp")
	private BigDecimal trxbaseImponible;

	@Column(name = "trx_valoriva")
	private BigDecimal trxValorIva;

	@Column(name = "trx_porciva")
	private BigDecimal trxPorcentajeIVa;
	
	@Column(name = "trx_detalle")
	private String trxDetalle;
	
	@Column(name = "trx_autorizacion")
	private String trxAutorizacion;
	
	@Column(name = "trx_transferencia")
	private String trxTransferencia;
	
	@Column(name = "trx_fechareg")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime trxFechaRegistro;
	
	@Column(name = "trx_fecharesp")
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime trxFechaRespuesta;

	@Column(name = "trx_codigoqr")
	private byte[] trxCodigoQR;
	
	@JoinColumn(name = "frp_secuencial", referencedColumnName = "frp_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private FormaDePago formaDepago;

	@JoinColumn(name = "ser_secuencial", referencedColumnName = "ser_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private Servicio servicio;

	@JoinColumn(name = "ciu_secuencial", referencedColumnName = "ciu_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private Ciudadano ciudadano;

	@JoinColumn(name = "rep_secuencial", referencedColumnName = "rep_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private ReservaParrilla reservaParrilla;

}
