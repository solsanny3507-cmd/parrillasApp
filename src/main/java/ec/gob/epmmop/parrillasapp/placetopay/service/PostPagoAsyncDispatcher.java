package ec.gob.epmmop.parrillasapp.placetopay.service;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class PostPagoAsyncDispatcher {

    private static final Logger LOGGER = Logger.getLogger(PostPagoAsyncDispatcher.class.getName());

    @Inject
    private PostPagoService postPagoService;

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void procesar(Integer requestId, String reference) {

        try {

            postPagoService.procesar(requestId, reference);

        } catch (Exception e) {

            /*
             * Importantísimo:
             *
             * Una excepción aquí NUNCA vuelve al
             * flujo que confirmó el pago.
             */
            LOGGER.log(Level.SEVERE, "Error en procesamiento postpago" + " | requestId=" + requestId, e);
        }
    }
}
