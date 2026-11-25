package ec.gob.epmmop.parrillasapp.general.repository;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;

import java.util.Optional;

@ApplicationScoped
public class TransaccionLockRepository {

    @Inject
    private EntityManager entityManager;

    /**
     * Obtiene la transacción aplicando un bloqueo pesimista
     * a nivel de base de datos.
     *
     * Mientras esta transacción esté bloqueada,
     * ningún otro procesamiento podrá modificarla
     * concurrentemente.
     */
    public Optional<Transaccion> obtenerParaProcesar(
            Integer requestId,
            String reference) {

        try {

            Transaccion transaccion = entityManager
                    .createQuery("""
                            SELECT t
                            FROM Transaccion t
                            WHERE t.trxRequestId = :requestId
                              AND t.trxReferencia = :reference
                            """, Transaccion.class)
                    .setParameter("requestId", requestId)
                    .setParameter("reference", reference)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)

                    /*
                     * Máximo de espera por el bloqueo.
                     *
                     * Evita que una petición quede esperando
                     * indefinidamente si otro proceso está
                     * trabajando la misma transacción.
                     */
                    .setHint("jakarta.persistence.lock.timeout", 10000)

                    .getSingleResult();

            return Optional.of(transaccion);

        } catch (NoResultException e) {

            return Optional.empty();
        }
    }
}
