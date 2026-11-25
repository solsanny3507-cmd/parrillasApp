package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.placetopay.model.AuditoriaPago;
import ec.gob.epmmop.parrillasapp.placetopay.repository.AuditoriaPagoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuditoriaPagoWriter {

    @Inject
    private AuditoriaPagoRepository repository;

    @Transactional(
            value = Transactional.TxType.REQUIRES_NEW,
            rollbackOn = Exception.class
    )
    public void guardar(
            AuditoriaPago auditoria) {

        repository.save(
                auditoria
        );
    }
}
