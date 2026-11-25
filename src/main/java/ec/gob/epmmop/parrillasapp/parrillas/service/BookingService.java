package ec.gob.epmmop.parrillasapp.parrillas.service;

import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import ec.gob.epmmop.parrillasapp.general.service.CiudadanoService;
import ec.gob.epmmop.parrillasapp.general.service.ServicioService;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PaymentRequest;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayRequest;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayResponse;
import ec.gob.epmmop.parrillasapp.placetopay.service.PaymentService;
import ec.gob.epmmop.parrillasapp.placetopay.service.PlacetoPayClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.net.UnknownHostException;

@ApplicationScoped
public class BookingService extends Utilitarios {

    @Inject
    private CiudadanoService ciudadanoService;

    @Inject
    private ServicioService servicioService;

    @Inject
    private PaymentService paymentService;

    @Inject
    private PlacetoPayClient placetoPayClient;

    private Ciudadano cliente;
    private Servicio servicio;
    private PaymentRequest paymentRequest;
    private PlacetoPayRequest placetoPayRequest;
    private PlacetoPayResponse placetoPayResponse;

    @Transactional(rollbackOn = Exception.class)
    public PlacetoPayResponse generarPago(Ciudadano ciudadano, Parrilla parrilla, DatosReservaDTO datosReserva) throws UnknownHostException {
        this.registrarPersona(ciudadano);
        this.generarRequestParaPlacetopay(ciudadano, parrilla);
        this.placetoPayRequest = this.paymentService.construirJsonSessionRequest(this.paymentRequest, datosReserva);
        this.placetoPayResponse = this.placetoPayClient.crearSesion(this.placetoPayRequest);

        return this.placetoPayResponse;
    }

    public void registrarPersona(Ciudadano ciudadano) {
        this.ciudadanoService.registrar(ciudadano);
    }

    public PaymentRequest generarRequestParaPlacetopay(Ciudadano ciudadano, Parrilla parrilla) throws UnknownHostException {
        this.servicio = this.servicioService.obtenerServicioParrilas();

        this.paymentRequest = new PaymentRequest();
        this.paymentRequest.setCiudadano(ciudadano);
        this.paymentRequest.setCorreo(ciudadano.getCiuCorreo());
        this.paymentRequest.setCodigoServicio(this.servicio.getSerCodigo());
        this.paymentRequest.setNombreServicio(this.servicio.getSerNombre() + " - " + parrilla.getGrupoParrilla().getParque().getParNombre());
        this.paymentRequest.setPrecio(this.servicio.getSerPrecio());
        this.paymentRequest.setIp(obtenerIP());

        return this.paymentRequest;
    }
}
