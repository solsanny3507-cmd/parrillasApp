package ec.gob.epmmop.parrillasapp.general.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class EntityManagerProducer {

    @PersistenceContext(unitName = "EpmmopPU")
    private EntityManager em;

    @Produces
    @ApplicationScoped
    public EntityManager produceEntityManager() {
        return em;
    }

    public void closeEntityManager(@Disposes EntityManager em) {
        // no hace falta cerrar, el contenedor lo maneja
    }

    // === ORACLE ===
    @PersistenceContext(unitName = "OraclePU")
    private EntityManager emOracle;

    @Produces
    @ApplicationScoped
    @OracleDB
    public EntityManager produceOracleEntityManager() {
        return emOracle;
    }

}
