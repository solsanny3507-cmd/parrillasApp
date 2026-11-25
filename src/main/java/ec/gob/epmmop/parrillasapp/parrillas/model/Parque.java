package ec.gob.epmmop.parrillasapp.parrillas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "eparque", schema = "parrillas")
public class Parque {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "par_secuencial")
	private Integer parSecuencial;

	@Column(name = "par_nombre")
	private String parNombre;
	
	@Column(name = "par_descripcion")
	private String parDescripcion;
	
	@Column(name = "par_frase")
	private String parFrase;
	
	@Column(name = "par_horario")
	private String parHorario;	
		
	@Column(name = "par_imagen")
	private String parImagen;
	
	@Column(name = "par_ubicacion")
	private String parUbicacion;
	
	@Column(name = "par_parrilla")
	private Boolean parParrilla;
	
	@Column(name = "par_estado")
	private Boolean parEstado;

	@Column(name = "par_codigosri")
	private String parCodigoSri;

}
