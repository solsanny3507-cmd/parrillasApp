package ec.gob.epmmop.parrillasapp.general.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eservicio", schema = "general")
public class Servicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ser_secuencial")
	private Integer serSecuencial;

	@Column(name = "ser_nombre")
	private String serNombre;

	@Column(name = "ser_codigo")
	private String serCodigo;

	@Column(name = "ser_precio")
	private BigDecimal serPrecio;

	@Column(name = "ser_iva")
	private Integer serIva;

	@Column(name = "ser_estado")
	private Boolean serEstado;
}
