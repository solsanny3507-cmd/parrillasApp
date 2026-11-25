package ec.gob.epmmop.parrillasapp.parrillas.controller;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.service.ReservaParrillaService;
import ec.gob.epmmop.parrillasapp.placetopay.dto.ResultadoProcesamientoPagoDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.OrigenProcesamientoPago;
import ec.gob.epmmop.parrillasapp.placetopay.service.ProcesamientoPagoService;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class CronjobController extends Utilitarios {

    @Inject
    private TransaccionService transaccionService;

    @Inject
    private ReservaParrillaService reservaParrillaService;

    @Inject
    private ProcesamientoPagoService procesamientoPagoService;

    private static final Logger log = Logger.getLogger(CronjobController.class.getName());

    // =========================================================
    // RECONCILIACIÓN GENERAL
    // =========================================================

    /*
     * Mecanismo adicional de seguridad.
     * Webhook y retorno siguen siendo los mecanismos
     * principales.
     */
    @Schedule(hour = "00", minute = "00", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void verificarTransaccionesPendientes() {

        log.info("***** INICIA CRONJOB TRANSACCIONES *****");

        List<Transaccion> pendientes = transaccionService.obtenerTransaccionesPendientes();

        if (pendientes == null || pendientes.isEmpty()) {

            log.info("No hay transacciones pendientes");

            return;
        }

        log.info("Cantidad de pendientes: " + pendientes.size());

        for (Transaccion tx : pendientes) {

            try {

                procesamientoPagoService.procesar(tx.getTrxRequestId(), tx.getTrxReferencia(), OrigenProcesamientoPago.CRON);

            } catch (Exception e) {

                log.log(Level.SEVERE, "Error procesando trx " + tx.getTrxRequestId(), e);
            }
        }
    }

    // =========================================================
    // RETENCIONES DE PAGO VENCIDAS
    // =========================================================

    /*
     * Corre una vez por minuto.
     *
     * IMPORTANTE:
     * NO expira directamente.
     *
     * Primero consulta PlaceToPay.
     */
    @Schedule(hour = "*", minute = "*", second = "20", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void verificarRetencionesPagoVencidas() {

        List<ReservaParrilla> retenciones = reservaParrillaService.obtenerRetencionesPagoVencidas();

        if (retenciones == null || retenciones.isEmpty()) {

            return;
        }

        log.info("Retenciones de pago vencidas por revisar: " + retenciones.size());

        for (ReservaParrilla reserva : retenciones) {

            try {

                Optional<Transaccion> txOpt = transaccionService.buscarPorReserva(reserva.getRepSecuencial());

                /*
                 * Caso excepcional:
                 *
                 * Se creó la retención pero nunca llegó a
                 * registrarse una sesión/transacción local.
                 */
                if (txOpt.isEmpty()) {

                    reservaParrillaService.liberarRetencion(reserva.getRepSecuencial(), "Retención vencida sin transacción de pago");

                    log.warning("Retención liberada sin transacción" + " | reserva=" + reserva.getRepSecuencial());

                    continue;
                }

                Transaccion tx = txOpt.get();

                ResultadoProcesamientoPagoDTO resultado = procesamientoPagoService.procesar(tx.getTrxRequestId(), tx.getTrxReferencia(), OrigenProcesamientoPago.CRON);

                String estadoPlaceToPay = resultado.getPagoResponse().getStatus();

                /*
                 * APPROVED:
                 * ProcesamientoPagoService ya confirmó
                 * pago + reserva.
                 *
                 * REJECTED:
                 * ProcesamientoPagoService ya canceló
                 * la reserva y la dejó inactiva.
                 *
                 * PENDING:
                 * NO hacemos nada.
                 */
                log.info("Retención reconciliada" + " | reserva=" + reserva.getRepSecuencial() + " | requestId=" + tx.getTrxRequestId() + " | PlaceToPay=" + estadoPlaceToPay);

            } catch (Exception e) {

                /*
                 * CRÍTICO:
                 *
                 * Un error de red o técnico JAMÁS
                 * libera la parrilla.
                 */
                log.log(Level.SEVERE, "No se pudo reconciliar retención" + " | reserva=" + reserva.getRepSecuencial() + ". Se mantiene bloqueada.", e);
            }
        }
    }

    // =========================================================
    // RESERVAS YA UTILIZADAS
    // =========================================================

    /*
     * Este cron es diferente.
     *
     * Expira reservas APROBADAS cuya fecha de utilización
     * ya quedó en el pasado.
     */
    @Schedule(hour = "00", minute = "10", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void verificarReservasExpiradas() {

        List<ReservaParrilla> reservasExpiradas = reservaParrillaService.obtenerReservasExpiradas();

        if (reservasExpiradas == null || reservasExpiradas.isEmpty()) {

            log.info("No hay reservas expiradas");

            return;
        }

        AtomicInteger contador = new AtomicInteger(0);

        reservasExpiradas.forEach(reserva -> {

            try {

                reservaParrillaService.modificarEstadoExpirado(reserva);

                contador.incrementAndGet();

            } catch (Exception e) {

                log.log(Level.SEVERE, "Error expirando reserva ID: " + reserva.getRepSecuencial(), e);
            }
        });

        log.info("Reservas expiradas correctamente: " + contador.get());
    }
}
