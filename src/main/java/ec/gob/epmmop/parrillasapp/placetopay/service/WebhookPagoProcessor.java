package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoTransaccion;
import ec.gob.epmmop.parrillasapp.placetopay.dto.ResultadoProcesamientoPagoDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.OrigenProcesamientoPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.WebhookPagoEvento;
import ec.gob.epmmop.parrillasapp.placetopay.repository.WebhookPagoEventoRepository;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class WebhookPagoProcessor {

    private static final Logger LOGGER = Logger.getLogger(WebhookPagoProcessor.class.getName());

    @Inject
    private WebhookPagoEventoRepository repository;

    @Inject
    private ProcesamientoPagoService procesamientoPagoService;

    @Inject
    private WebhookPagoEventoEstadoService webhookPagoEventoEstadoService;

    @Schedule(hour = "*", minute = "*", second = "*/15", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void procesarPendientes() {

        List<WebhookPagoEvento> pendientes = repository.obtenerPendientes();

        if (pendientes == null || pendientes.isEmpty()) {

            return;
        }

        pendientes.stream().limit(50).forEach(this::procesarEvento);
    }

    private void procesarEvento(WebhookPagoEvento evento) {

        try {

            ResultadoProcesamientoPagoDTO resultado = procesamientoPagoService.procesar(evento.getRequestId(), evento.getReference(), OrigenProcesamientoPago.WEBHOOK);

            String estado = resultado.getTransaccion().getTrxEstadoTransaccion();

            /*
             * Solo eliminamos el evento de la cola lógica
             * cuando conocemos un resultado financiero final.
             */
            if (esEstadoFinal(estado)) {

                webhookPagoEventoEstadoService.marcarProcesado(evento);

                LOGGER.info("Webhook procesado" + " | requestId=" + evento.getRequestId() + " | estado=" + estado);

            } else {

                LOGGER.info("Webhook todavía sin estado final" + " | requestId=" + evento.getRequestId() + " | estado=" + estado);
            }

        } catch (Exception e) {

            webhookPagoEventoEstadoService.marcarError(evento, e.getMessage());

            LOGGER.log(Level.SEVERE, "Error procesando webhook" + " | requestId=" + evento.getRequestId(), e);
        }
    }

    private boolean esEstadoFinal(String estado) {

        if (estado == null) {
            return false;
        }

        return EstadoTransaccion.APROBADO.getDescripcion().equalsIgnoreCase(estado) || EstadoTransaccion.RECHAZADO.getDescripcion().equalsIgnoreCase(estado);
    }
}
