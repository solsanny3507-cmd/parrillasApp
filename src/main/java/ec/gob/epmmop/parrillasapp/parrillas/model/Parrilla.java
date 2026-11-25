package ec.gob.epmmop.parrillasapp.parrillas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eparrilla", schema = "parrillas")
public class Parrilla {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "asa_secuencial")
	private Integer asaSecuencial;

	@Column(name = "asa_nombre")
	private String asaNombre;

	@Column(name = "asa_detalle")
	private String asaDetalle;

	@Column(name = "asa_capacidad")
	private Integer asaCapacidad;

	@Column(name = "asa_imagen")
	private String asaImagen;

	@Column(name = "asa_latitud")
	private BigDecimal asaLatitud;

	@Column(name = "asa_longitud")
	private BigDecimal asaLongitud;

	@Column(name = "asa_estado")
	private Boolean asaEstado;
	
	@JoinColumn(name = "gra_secuencial", referencedColumnName = "gra_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private GrupoParrilla grupoParrilla;
}
