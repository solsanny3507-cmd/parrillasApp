package ec.gob.epmmop.parrillasapp.parrillas.service;

import ec.gob.epmmop.parrillasapp.parrillas.model.Parque;
import ec.gob.epmmop.parrillasapp.parrillas.model.Parrilla;
import ec.gob.epmmop.parrillasapp.parrillas.repository.ParrillaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ParrillaService {

    @Inject
    private ParrillaRepository parrillaRepository;

    public List<Parrilla> listarParrillasDisponibles(Parque parque, LocalDate fechaReserva) {

        var listaParrillasDisponibles = parrillaRepository.listarParrillasDisponibles(parque.getParSecuencial(), fechaReserva)
                .stream()
                .filter(Parrilla::getAsaEstado)
                .toList();

        return listaParrillasDisponibles;
    }

    public Parrilla obtenerParrilla(Integer idParrilla) {
        return parrillaRepository.obtenerParrilla(idParrilla);
    }
}
