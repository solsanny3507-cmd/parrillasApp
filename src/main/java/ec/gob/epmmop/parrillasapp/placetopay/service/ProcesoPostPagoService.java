package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.repository.TransaccionRepository;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.model.EstadoProcesoPostPago;
import ec.gob.epmmop.parrillasapp.placetopay.model.ProcesoPostPago;
import ec.gob.epmmop.parrillasapp.placetopay.repository.ProcesoPostPagoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ProcesoPostPagoService {

    private static final int MINUTOS_BLOQUEO_EXPIRADO = 5;

    @Inject
    private ProcesoPostPagoRepository repository;

    @Inject
    private TransaccionRepository transaccionRepository;

    @Inject
    private EntityManager entityManager;

    /**
     * MUY IMPORTANTE:
     * <p>
     * Este método debe ejecutarse dentro de la misma transacción
     * que confirma el pago.
     * <p>
     * Pago APROBADO + Reserva RESERVADA + PostPago PENDIENTE
     * se confirman todos juntos.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public void registrarPendiente(Transaccion transaccion, PagoResponseDTO pago) {

        List<ProcesoPostPago> existentes = repository.buscarPorPago(transaccion.getTrxRequestId(), transaccion.getTrxReferencia());

        if (!existentes.isEmpty()) {
            return;
        }

        ProcesoPostPago proceso = new ProcesoPostPago();

        proceso.setRequestId(transaccion.getTrxRequestId());

        proceso.setReference(transaccion.getTrxReferencia());

        // Snapshot PlaceToPay
        proceso.setStatusPlaceToPay(pago.getStatus());

        proceso.setAuthorization(pago.getAuthorization());

        proceso.setNombreTarjeta(pago.getNombreTarjeta());

        proceso.setReason(pago.getReason());

        proceso.setMessage(pago.getMessage());

        proceso.setFechaPago(pago.getDate());

        String pendiente = EstadoProcesoPostPago.PENDIENTE.name();

        proceso.setEstadoFactura(pendiente);
        proceso.setEstadoQr(pendiente);
        proceso.setEstadoCorreo(pendiente);
        proceso.setEstadoNotificacion(pendiente);

        proceso.setIntentosFactura(0);
        proceso.setIntentosQr(0);
        proceso.setIntentosCorreo(0);
        proceso.setIntentosNotificacion(0);

        proceso.setEnProceso(Boolean.FALSE);

        proceso.setFechaRegistro(LocalDateTime.now());

        proceso.setFechaActualizacion(LocalDateTime.now());

        repository.save(proceso);
    }

    public ProcesoPostPago obtener(Integer requestId, String reference) {

        List<ProcesoPostPago> lista = repository.buscarPorPago(requestId, reference);

        return lista.isEmpty() ? null : lista.get(0);
    }

    public ProcesoPostPago obtener(Long id) {
        return repository.findBy(id);
    }

    public List<ProcesoPostPago> obtenerProcesables() {
        return repository.obtenerProcesables();
    }

    // ==========================================================
    // CLAIM
    // ==========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public boolean reclamar(Long id) {

        ProcesoPostPago proceso = entityManager.find(ProcesoPostPago.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (proceso == null) {
            return false;
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (Boolean.TRUE.equals(proceso.getEnProceso())) {

            LocalDateTime inicio = proceso.getFechaInicioProceso();

            /*
             * Si otro proceso lo tomó recientemente,
             * no hacemos nada.
             */
            if (inicio != null && inicio.isAfter(ahora.minusMinutes(MINUTOS_BLOQUEO_EXPIRADO))) {

                return false;
            }

            /*
             * Si lleva más de 5 minutos bloqueado,
             * asumimos que WildFly/proceso murió y
             * permitimos recuperación.
             */
        }

        proceso.setEnProceso(Boolean.TRUE);
        proceso.setFechaInicioProceso(ahora);
        proceso.setFechaActualizacion(ahora);

        return true;
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void liberar(Long id) {

        ProcesoPostPago proceso = entityManager.find(ProcesoPostPago.class, id, LockModeType.PESSIMISTIC_WRITE);

        if (proceso == null) {
            return;
        }

        proceso.setEnProceso(Boolean.FALSE);
        proceso.setFechaInicioProceso(null);
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    // ==========================================================
    // FACTURA
    // ==========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void facturaCompletada(Long id) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoFactura(EstadoProcesoPostPago.COMPLETADO.name());

        proceso.setErrorFactura(null);
        proceso.setFechaFactura(LocalDateTime.now());
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void facturaError(Long id, String error) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoFactura(EstadoProcesoPostPago.ERROR.name());

        proceso.setIntentosFactura(numero(proceso.getIntentosFactura()) + 1);

        proceso.setErrorFactura(limitar(error));

        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    // ==========================================================
    // QR
    // ==========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Exception.class)
    public void qrCompletado(Long id, Integer requestId, String reference, byte[] qr) {

        Transaccion transaccion = transaccionRepository.obtenerTransaccionPorRequestIdAndReference(requestId, reference);

        if (transaccion == null) {
            throw new IllegalStateException("No existe la transacción para guardar QR");
        }

        /*
         * Ahora el QR se guarda EXPLÍCITAMENTE
         * en la transacción.
         */
        transaccion.setTrxCodigoQR(qr);

        transaccionRepository.save(transaccion);

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoQr(EstadoProcesoPostPago.COMPLETADO.name());

        proceso.setErrorQr(null);
        proceso.setFechaQr(LocalDateTime.now());
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void qrError(Long id, String error) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoQr(EstadoProcesoPostPago.ERROR.name());

        proceso.setIntentosQr(numero(proceso.getIntentosQr()) + 1);

        proceso.setErrorQr(limitar(error));

        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    // ==========================================================
    // CORREO
    // ==========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void correoCompletado(Long id) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoCorreo(EstadoProcesoPostPago.COMPLETADO.name());

        proceso.setErrorCorreo(null);
        proceso.setFechaCorreo(LocalDateTime.now());
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void correoError(Long id, String error) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoCorreo(EstadoProcesoPostPago.ERROR.name());

        proceso.setIntentosCorreo(numero(proceso.getIntentosCorreo()) + 1);

        proceso.setErrorCorreo(limitar(error));

        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    private ProcesoPostPago obtenerObligatorio(Long id) {

        ProcesoPostPago proceso = entityManager.find(ProcesoPostPago.class, id);

        if (proceso == null) {
            throw new IllegalStateException("No existe proceso postpago id=" + id);
        }

        return proceso;
    }

    private int numero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private String limitar(String error) {

        if (error == null) {
            return "Error no especificado";
        }

        return error.length() > 2000 ? error.substring(0, 2000) : error;
    }

    // ==========================================================
    // NOTIFICACIÓN MI QUITO
    // ==========================================================

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void notificacionCompletada(Long id) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoNotificacion(
                EstadoProcesoPostPago.COMPLETADO.name()
        );

        proceso.setIntentosNotificacion(
                numero(proceso.getIntentosNotificacion()) + 1
        );

        proceso.setErrorNotificacion(null);
        proceso.setFechaNotificacion(LocalDateTime.now());
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void notificacionSinSesion(Long id) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoNotificacion("SIN_SESION");

        proceso.setIntentosNotificacion(
                numero(proceso.getIntentosNotificacion()) + 1
        );

        proceso.setErrorNotificacion(null);
        proceso.setFechaNotificacion(LocalDateTime.now());
        proceso.setFechaActualizacion(LocalDateTime.now());
    }

    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public void notificacionError(
            Long id,
            String error) {

        ProcesoPostPago proceso = obtenerObligatorio(id);

        proceso.setEstadoNotificacion(
                EstadoProcesoPostPago.ERROR.name()
        );

        proceso.setIntentosNotificacion(
                numero(proceso.getIntentosNotificacion()) + 1
        );

        proceso.setErrorNotificacion(
                limitar(error)
        );

        proceso.setFechaActualizacion(
                LocalDateTime.now()
        );
    }
}
