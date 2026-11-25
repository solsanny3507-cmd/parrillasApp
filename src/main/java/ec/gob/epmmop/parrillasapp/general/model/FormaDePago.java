package ec.gob.epmmop.parrillasapp.general.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eformapago", schema = "general")
public class FormaDePago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "frp_secuencial")
	private Integer frpSecuencial;
	
	@Column(name = "frp_nombre")
	private String frpNombre;
	
	@Column(name = "frp_tiposri")
	private String  frpTipoSri;
	
	@Column(name = "frp_descripago")
	private String  frpDescripcion;
	
	@Column(name = "frp_estado")
	private Boolean  frpEstado;

}
