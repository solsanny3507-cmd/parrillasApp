package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.placetopay.model.*;
import ec.gob.epmmop.parrillasapp.placetopay.repository.AuditoriaPagoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AuditoriaPagoService {

    private static final Logger LOGGER = Logger.getLogger(AuditoriaPagoService.class.getName());

    @Inject
    private AuditoriaPagoWriter writer;

    @Inject
    private AuditoriaPagoRepository repository;

    /**
     * Registro seguro.
     * <p>
     * Una falla de auditoría NUNCA debe afectar
     * el procesamiento principal del pago.
     */
    public void registrar(Integer requestId, String reference, OrigenProcesamientoPago origen, TipoEventoPago evento, String estadoPlaceToPay, String estadoAnterior, String estadoNuevo, ResultadoAuditoriaPago resultado, String mensaje, Throwable error) {

        try {

            AuditoriaPago auditoria = new AuditoriaPago();

            auditoria.setRequestId(requestId);

            auditoria.setReference(limitar(reference, 150));

            auditoria.setOrigen(origen != null ? origen.name() : "SISTEMA");

            auditoria.setEvento(evento != null ? evento.name() : "EVENTO_DESCONOCIDO");

            auditoria.setEstadoPlaceToPay(limitar(estadoPlaceToPay, 30));

            auditoria.setEstadoAnterior(limitar(estadoAnterior, 30));

            auditoria.setEstadoNuevo(limitar(estadoNuevo, 30));

            auditoria.setResultado(resultado != null ? resultado.name() : ResultadoAuditoriaPago.ERROR.name());

            auditoria.setMensaje(limitar(mensaje, 1000));

            auditoria.setError(error != null ? limitar(construirError(error), 2000) : null);

            auditoria.setFecha(LocalDateTime.now());

            /*
             * Se ejecutará en REQUIRES_NEW.
             */
            writer.guardar(auditoria);

        } catch (Exception e) {

            /*
             * CRÍTICO:
             *
             * Este error NO se propaga.
             *
             * Si falla la auditoría, el pago debe
             * continuar normalmente.
             */
            LOGGER.log(Level.SEVERE, "No fue posible registrar auditoría" + " | requestId=" + requestId + " | evento=" + evento, e);
        }
    }

    public List<AuditoriaPago> obtenerHistorial(Integer requestId) {

        return repository.obtenerHistorial(requestId);
    }

    public List<AuditoriaPago> obtenerHistorialPorReferencia(String reference) {

        return repository.obtenerHistorialPorReferencia(reference);
    }

    private String construirError(Throwable error) {

        StringBuilder resultado = new StringBuilder();

        Throwable actual = error;

        int nivel = 0;

        while (actual != null && nivel < 5) {

            if (nivel > 0) {
                resultado.append(" <- ");
            }

            resultado.append(actual.getClass().getSimpleName());

            if (actual.getMessage() != null && !actual.getMessage().isBlank()) {

                resultado.append(": ").append(actual.getMessage());
            }

            actual = actual.getCause();

            nivel++;
        }

        return resultado.toString();
    }

    private String limitar(String valor, int longitudMaxima) {

        if (valor == null) {
            return null;
        }

        return valor.length() <= longitudMaxima ? valor : valor.substring(0, longitudMaxima);
    }
}
