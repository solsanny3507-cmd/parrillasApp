package ec.gob.epmmop.parrillasapp.placetopay.repository;

import ec.gob.epmmop.parrillasapp.placetopay.model.ProcesoPostPago;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

import java.util.List;

@Repository
public interface ProcesoPostPagoRepository
        extends EntityRepository<ProcesoPostPago, Long> {

    @Query("""
            SELECT p
            FROM ProcesoPostPago p
            WHERE p.requestId = :requestId
              AND p.reference = :reference
            """)
    List<ProcesoPostPago> buscarPorPago(
            @QueryParam("requestId") Integer requestId,
            @QueryParam("reference") String reference
    );

    @Query("""
        SELECT p
        FROM ProcesoPostPago p
        WHERE
            (
                p.estadoFactura IN ('PENDIENTE', 'ERROR')
                AND p.intentosFactura < 5
            )
            OR
            (
                p.estadoQr IN ('PENDIENTE', 'ERROR')
                AND p.intentosQr < 5
            )
            OR
            (
                p.estadoCorreo IN ('PENDIENTE', 'ERROR')
                AND p.intentosCorreo < 5
            )
            OR
            (
                p.estadoNotificacion = 'PENDIENTE'
                AND p.estadoQr = 'COMPLETADO'
            )
        ORDER BY p.fechaRegistro
        """)
    List<ProcesoPostPago> obtenerProcesables();
}
