package ec.gob.epmmop.parrillasapp.placetopay.repository;

import ec.gob.epmmop.parrillasapp.placetopay.model.WebhookPagoEvento;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

import java.util.List;

@Repository
public interface WebhookPagoEventoRepository
        extends EntityRepository<WebhookPagoEvento, Long> {

    @Query("""
            SELECT e
            FROM WebhookPagoEvento e
            WHERE e.estado = 'PENDIENTE'
            ORDER BY e.fechaRegistro
            """)
    List<WebhookPagoEvento> obtenerPendientes();
}
