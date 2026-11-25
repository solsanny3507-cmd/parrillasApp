package ec.gob.epmmop.parrillasapp.parrillas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "egruparrilla", schema = "parrillas")
public class GrupoParrilla {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "gra_secuencial")
	private Integer graSecuencial;

	@Column(name = "gra_nombre")
	private String graNombre;
	
	@Column(name = "gra_estado")
	private Boolean graEstado;

	@JoinColumn(name = "par_secuencial", referencedColumnName = "par_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private Parque parque;

}
