package ec.gob.epmmop.parrillasapp.general.service;

import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.general.repository.FacturaRepository;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;

@ApplicationScoped
public class FacturaService {

    @Inject
    private FacturaRepository facturaRepository;

    public void enviarFactura(Transaccion transaccion, PagoResponseDTO pagoResponseDTO) {

        if (transaccion == null) {
            throw new IllegalArgumentException("La transacción no puede ser null");
        }

        String cedulaCliente = transaccion.getCiudadano().getCiuIdentificacion();

        String nombreCliente = transaccion.getCiudadano().getCiuNombre().toUpperCase().trim();

        String correoCliente = transaccion.getCiudadano().getCiuCorreo();

        String telefonoCliente = transaccion.getCiudadano().getCiuTelefono();

        BigDecimal totalPago = transaccion.getTrxMonto();

        BigDecimal baseImponible = transaccion.getTrxbaseImponible();

        BigDecimal valorIva = transaccion.getTrxValorIva();

        BigDecimal porcentajeIVA = transaccion.getTrxPorcentajeIVa();

        String nombreParque = transaccion.getReservaParrilla().getParrilla().getGrupoParrilla().getParque().getParNombre().toUpperCase().trim();

        String nombreTarjeta = obtenerNombreTarjeta(pagoResponseDTO);

        String autorizacion = obtenerAutorizacion(pagoResponseDTO);

        String puntoRecaudacion = transaccion.getReservaParrilla().getParrilla().getGrupoParrilla().getParque().getParCodigoSri();

        facturaRepository.crearFacturaElectronica(
                cedulaCliente,
                nombreCliente,
                correoCliente,
                telefonoCliente,
                totalPago,
                valorIva,
                baseImponible,
                porcentajeIVA,
                nombreParque,
                nombreTarjeta,
                autorizacion,
                puntoRecaudacion
        );
    }


    private String obtenerNombreTarjeta(
            PagoResponseDTO pagoResponseDTO) {

        if (pagoResponseDTO == null
            || pagoResponseDTO.getNombreTarjeta() == null
            || pagoResponseDTO.getNombreTarjeta().isBlank()) {

            throw new IllegalStateException(
                    "No se puede generar la factura: "
                    + "PlaceToPay no proporcionó el nombre de la tarjeta"
            );
        }

        return pagoResponseDTO
                .getNombreTarjeta()
                .trim()
                .toUpperCase();
    }

    private String obtenerAutorizacion(
            PagoResponseDTO pagoResponseDTO) {

        if (pagoResponseDTO == null
            || pagoResponseDTO.getAuthorization() == null
            || pagoResponseDTO.getAuthorization().isBlank()) {

            throw new IllegalStateException(
                    "No se puede generar la factura: "
                    + "no existe autorización de PlaceToPay"
            );
        }

        return pagoResponseDTO
                .getAuthorization()
                .trim();
    }
}
