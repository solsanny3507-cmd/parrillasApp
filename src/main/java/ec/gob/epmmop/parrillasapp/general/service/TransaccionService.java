package ec.gob.epmmop.parrillasapp.general.service;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.repository.TransaccionRepository;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoReserva;
import ec.gob.epmmop.parrillasapp.parrillas.model.EstadoTransaccion;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.service.ReservaParrillaService;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TransaccionService extends Utilitarios {

    @Inject
    private TransaccionRepository transaccionRepository;

    @Inject
    private ReservaParrillaService reservaParrillaService;

    @Transactional(rollbackOn = Exception.class)
    public Transaccion registrar(ReservaParrilla reservaParrilla, Servicio servicio, PlacetoPayResponse placetoPayResponse, DatosReservaDTO datosReservaDTO) {
        Transaccion transaccion = new Transaccion();
        transaccion.setReservaParrilla(reservaParrilla);
        transaccion.setServicio(servicio);
        transaccion.setCiudadano(datosReservaDTO.getCiudadano());
        transaccion.setTrxRequestId(Integer.valueOf(placetoPayResponse.getRequestId()));
        transaccion.setTrxReferencia(placetoPayResponse.getReference());
        transaccion.setTrxEstadoTransaccion(EstadoTransaccion.PENDIENTE.getDescripcion());
        transaccion.setTrxUrl(placetoPayResponse.getProcessUrl());
        transaccion.setTrxMonto(datosReservaDTO.getPrecioTotal());
        transaccion.setTrxbaseImponible(datosReservaDTO.getBaseImponible());
        transaccion.setTrxValorIva(datosReservaDTO.getValorIVA());
        transaccion.setTrxPorcentajeIVa(datosReservaDTO.getIvaPorcentaje());
        transaccion.setTrxDetalle(placetoPayResponse.getDescription());
        transaccion.setTrxFechaRegistro(obtenerFechaActual());
        transaccionRepository.save(transaccion);

        return transaccion;
    }

    @Transactional(rollbackOn = Exception.class)
    public Transaccion modificarTransaccionReserva(Transaccion transaccion, PagoResponseDTO pagoResponseDTO) {

        String estado = transformarStatusReserva(pagoResponseDTO.getStatus());

        transaccion.setTrxEstadoTransaccion(estado);

        if (pagoResponseDTO.getAuthorization() != null && !pagoResponseDTO.getAuthorization().isBlank()) {

            transaccion.setTrxAutorizacion(pagoResponseDTO.getAuthorization());
        }

        if (pagoResponseDTO.getDate() != null && !pagoResponseDTO.getDate().isBlank()) {

            transaccion.setTrxFechaRespuesta(convertirStringToDate(pagoResponseDTO.getDate()).toLocalDateTime());
        }

        transaccion = transaccionRepository.save(transaccion);

        /*
         * SOLO actualizamos la reserva.
         *
         * Aquí ya NO:
         * - facturamos
         * - generamos QR
         * - enviamos correo
         */
        reservaParrillaService.modificarReserva(transaccion.getReservaParrilla(), pagoResponseDTO);

        return transaccion;
    }

    public Transaccion obtenerTransaccion(Integer requestId) {
        return transaccionRepository.obtenerTransaccionPorRequestId(requestId);
    }

    public Transaccion obtenerTransaccion(Integer requestId, String reference) {
        return transaccionRepository.obtenerTransaccionPorRequestIdAndReference(requestId, reference);
    }

    public Optional<Transaccion> buscarTransaccionPendiente(Ciudadano ciudadano) {
        List<Transaccion> lista = transaccionRepository.buscarTransaccionPendiente(ciudadano.getCiuSecuencial());
        return lista.stream().findFirst();
    }

    public List<Transaccion> obtenerTransaccionesPorCliente(Ciudadano ciudadano) {
        return transaccionRepository.buscarTransaccionPorCliente(ciudadano.getCiuSecuencial());
    }

    public List<Transaccion> obtenerTransaccionesPendientes() {
        return transaccionRepository.obtenerTransaccionesPendientes();
    }

    @Transactional
    public void modificarTransaccionQR(Transaccion transaccion) {
        transaccionRepository.save(transaccion);
    }

    public Transaccion obtenerTransaccionCompleta(Integer requestId, String reference) {
        return transaccionRepository.obtenerTransaccionCompleta(requestId, reference);
    }

    public Optional<Transaccion> buscarPorReserva(Integer idReserva) {

        List<Transaccion> lista = transaccionRepository.buscarPorReserva(idReserva);

        if (lista == null || lista.isEmpty()) {

            return Optional.empty();
        }

        return Optional.of(lista.get(0));
    }

    public List<Transaccion> obtenerReservasActivasPorIdentificacion(
            String identificacion) {

        return transaccionRepository
                .buscarReservasActivasPorIdentificacion(
                        identificacion,
                        EstadoTransaccion.APROBADO.getDescripcion(),
                        EstadoReserva.RESERVADA.getDescripcion()
                );
    }
}
