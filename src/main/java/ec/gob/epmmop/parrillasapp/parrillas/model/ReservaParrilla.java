package ec.gob.epmmop.parrillasapp.parrillas.model;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ereservparri", schema = "parrillas")
public class ReservaParrilla {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rep_secuencial")
	private Integer repSecuencial;

	@Column(name = "rep_fechareg")
	private LocalDateTime repFechaRegistro;
	
	@Column(name = "rep_fechareserva")
	private LocalDate repFechaReserva;
	
	@Column(name = "rep_activo")
	private Boolean repActivo;

	@Column(name = "rep_estado_pago")
	private String repEstadoPago;

	@Column(name = "rep_observacion")
	private String repObservacion;

	@Column(name = "rep_fechaexpira")
	private LocalDateTime repFechaExpiracion;
	
	@JoinColumn(name = "asa_secuencial", referencedColumnName = "asa_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private Parrilla parrilla;
	
	@JoinColumn(name = "ciu_secuencial", referencedColumnName = "ciu_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private Ciudadano ciudadano;
}
