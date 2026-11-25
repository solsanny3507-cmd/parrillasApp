package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.placetopay.model.ProcesoPostPago;
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
public class PostPagoProcessor {

    private static final Logger LOGGER = Logger.getLogger(PostPagoProcessor.class.getName());

    private static final int MAXIMO_POR_EJECUCION = 20;

    @Inject
    private ProcesoPostPagoService procesoPostPagoService;

    @Inject
    private PostPagoService postPagoService;

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void procesarPendientes() {

        List<ProcesoPostPago> procesos = procesoPostPagoService.obtenerProcesables();

        if (procesos == null || procesos.isEmpty()) {

            return;
        }

        procesos.stream().limit(MAXIMO_POR_EJECUCION).forEach(proceso -> {

            try {

                postPagoService.procesar(proceso.getSecuencial());

            } catch (Exception e) {

                LOGGER.log(Level.SEVERE, "Error ejecutando postpago" + " | id=" + proceso.getSecuencial(), e);
            }
        });
    }
}
