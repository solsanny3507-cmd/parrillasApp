package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.placetopay.model.WebhookPagoEvento;
import ec.gob.epmmop.parrillasapp.placetopay.repository.WebhookPagoEventoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class WebhookPagoEventoEstadoService {

    @Inject
    private WebhookPagoEventoRepository repository;

    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public void marcarProcesado(WebhookPagoEvento evento) {

        evento.setEstado("PROCESADO");
        evento.setFechaProceso(LocalDateTime.now());
        evento.setUltimoError(null);

        repository.save(evento);
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public void marcarError(WebhookPagoEvento evento, String error) {

        evento.setIntentos(evento.getIntentos() == null ? 1 : evento.getIntentos() + 1);

        String mensaje = error != null ? error : "Error no especificado";

        if (mensaje.length() > 1000) {
            mensaje = mensaje.substring(0, 1000);
        }

        evento.setUltimoError(mensaje);

        /*
         * NO cambiamos estado a ERROR.
         *
         * Permanece PENDIENTE para que
         * pueda volver a intentarse.
         */
        repository.save(evento);
    }
}
