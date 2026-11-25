package ec.gob.epmmop.parrillasapp.api.service;

import ec.gob.epmmop.parrillasapp.api.dto.ReservaActivaDTO;
import ec.gob.epmmop.parrillasapp.api.dto.ReservaActivaResponseDTO;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.service.TransaccionService;
import ec.gob.epmmop.parrillasapp.parrillas.model.GrupoParrilla;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parque;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class ReservaConsultaService {

    private static final String QR_MIME_TYPE =
            "image/png";

    @Inject
    private TransaccionService transaccionService;

    public ReservaActivaResponseDTO consultarReservasActivas(
            String identificacion) {

        String identificacionNormalizada =
                normalizarIdentificacion(
                        identificacion
                );

        List<Transaccion> transacciones =
                transaccionService
                        .obtenerReservasActivasPorIdentificacion(
                                identificacionNormalizada
                        );

        List<ReservaActivaDTO> reservas =
                transacciones.stream()
                        .map(this::convertirDTO)
                        .toList();

        return new ReservaActivaResponseDTO(
                identificacionNormalizada,
                reservas
        );
    }

    private ReservaActivaDTO convertirDTO(
            Transaccion transaccion) {

        ReservaParrilla reserva =
                transaccion.getReservaParrilla();

        Parrilla parrilla =
                reserva.getParrilla();

        GrupoParrilla grupoParrilla =
                parrilla.getGrupoParrilla();

        Parque parque =
                grupoParrilla.getParque();

        ReservaActivaDTO dto =
                new ReservaActivaDTO();

//        dto.setReservaId(
//                reserva.getRepSecuencial()
//        );

        dto.setFechaReserva(
                reserva.getRepFechaReserva() != null
                        ? reserva.getRepFechaReserva().toString()
                        : null
        );

        dto.setEstado(
                reserva.getRepEstadoPago()
        );

        dto.setParque(
                parque.getParNombre()
        );

//        dto.setUbicacionParque(
//                parque.getParUbicacion()
//        );

        dto.setGrupoParrilla(
                grupoParrilla.getGraNombre()
        );

//        dto.setParrillaId(
//                parrilla.getAsaSecuencial()
//        );

        dto.setParrilla(
                parrilla.getAsaNombre()
        );

//        dto.setDetalleParrilla(
//                parrilla.getAsaDetalle()
//        );

//        dto.setCapacidad(
//                parrilla.getAsaCapacidad()
//        );

        dto.setRequestId(
                transaccion.getTrxRequestId()
        );

        dto.setReferenciaPago(
                transaccion.getTrxReferencia()
        );

        dto.setMonto(
                transaccion.getTrxMonto()
        );

        dto.setQrMimeType(
                QR_MIME_TYPE
        );

        dto.setQrBase64(
                convertirQrBase64(
                        transaccion.getTrxCodigoQR()
                )
        );

        return dto;
    }

    private String convertirQrBase64(
            byte[] qr) {

        /*
         * La consulta ya exige trxCodigoQR IS NOT NULL.
         *
         * Esta validación adicional protege el contrato frente
         * a datos históricos o inconsistentes.
         */
        if (qr == null
            || qr.length == 0) {

            throw new IllegalStateException(
                    "La reserva no dispone de código QR"
            );
        }

        return Base64
                .getEncoder()
                .encodeToString(qr);
    }

    private String normalizarIdentificacion(
            String identificacion) {

        if (identificacion == null) {
            return null;
        }

        return identificacion.trim();
    }
}