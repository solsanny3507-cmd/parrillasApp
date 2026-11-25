package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.config.OracleDB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import java.math.BigDecimal;

@ApplicationScoped
public class FacturaRepository {

    @Inject
    @OracleDB
    private EntityManager em;

    public void crearFacturaElectronica(
            String cedula,
            String persona,
            String correo,
            String telefono,
            BigDecimal valor,
            BigDecimal iva,
            BigDecimal base,
            BigDecimal porIva,
            String nombreParque,
            String nombreTarjeta,
            String numeroAutorizacion,
            String puntoRecaudacion
    ) {

        try {
            StoredProcedureQuery query = em.createStoredProcedureQuery("ftp_crea_facelec");

            // Registrar parámetros
            query.registerStoredProcedureParameter("p_cedula", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_persona", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_correo", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_telefono", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_valor", BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_iva", BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_base", BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_poriva", BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_parque", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_tarjeta", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_autoriza", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_recaudacion", String.class, ParameterMode.IN);

            // Setear valores
            query.setParameter("p_cedula", cedula);
            query.setParameter("p_persona", persona);
            query.setParameter("p_correo", correo);
            query.setParameter("p_telefono", telefono);
            query.setParameter("p_valor", valor);
            query.setParameter("p_iva", iva);
            query.setParameter("p_base", base);
            query.setParameter("p_poriva", porIva);
            query.setParameter("p_parque", nombreParque);
            query.setParameter("p_tarjeta", nombreTarjeta);
            query.setParameter("p_autoriza", numeroAutorizacion);
            query.setParameter("p_recaudacion", puntoRecaudacion);

            // Ejecutar
            query.execute();

        } catch (Exception e) {
            throw new RuntimeException("Error ejecutando SP ftp_crea_facelec", e);
        }
    }
}
