package ec.gob.epmmop.parrillasapp.placetopay.repository;

import ec.gob.epmmop.parrillasapp.placetopay.model.AuditoriaPago;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.util.List;

@Repository
public interface AuditoriaPagoRepository
        extends EntityRepository<AuditoriaPago, Long> {

    @Query("""
            SELECT a
            FROM AuditoriaPago a
            WHERE a.requestId = :requestId
            ORDER BY a.fecha ASC,
                     a.secuencial ASC
            """)
    List<AuditoriaPago> obtenerHistorial(
            @QueryParam("requestId")
            Integer requestId
    );

    @Query("""
            SELECT a
            FROM AuditoriaPago a
            WHERE a.reference = :reference
            ORDER BY a.fecha ASC,
                     a.secuencial ASC
            """)
    List<AuditoriaPago> obtenerHistorialPorReferencia(
            @QueryParam("reference")
            String reference
    );
}
