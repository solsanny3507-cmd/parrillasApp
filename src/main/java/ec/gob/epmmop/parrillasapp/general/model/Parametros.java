package ec.gob.epmmop.parrillasapp.general.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eparams", schema = "general")
public class Parametros {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "prm_secuencial")
	private Integer prmSecuencial;
	
	@Column(name = "prm_login")
	private String prmLogin;
	
	@Column(name = "prm_secretkey")
	private String prmSecretKey;

	@Column(name = "prm_url")
	private String prmUrl;
	
	@Column(name = "prm_url_return")
	private String prmUrlReturn;
	
	@Column(name = "prm_url_cancel")
	private String prmUrlCancel;

	@Column(name = "prm_estado")
	private Boolean  prmEstado;
	
	@JoinColumn(name = "frp_secuencial", referencedColumnName = "frp_secuencial")
	@ManyToOne(fetch = FetchType.EAGER)
	private FormaDePago formaDepago;

}
