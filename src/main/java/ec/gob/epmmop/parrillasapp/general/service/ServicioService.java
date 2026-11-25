package ec.gob.epmmop.parrillasapp.general.service;

import ec.gob.epmmop.parrillasapp.general.model.Servicio;
import ec.gob.epmmop.parrillasapp.general.repository.ServicioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


@ApplicationScoped
public class ServicioService {

    @Inject
    private ServicioRepository servicioRespository;

    public Servicio obtenerServicioParrilas() {
        return this.servicioRespository.obtenerServicioParrilas();
    }
}
