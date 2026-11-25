package ec.gob.epmmop.parrillasapp.parrillas.service;

import ec.gob.epmmop.parrillasapp.parrillas.model.Parque;
import ec.gob.epmmop.parrillasapp.parrillas.repository.ParqueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class ParqueService {

    @Inject
    private ParqueRepository repo;

    public List<Parque> listarParquesBBQ() {
        var listaParquesBBQ = repo.findAll()
                .stream()
                .filter(Parque::getParEstado)
                .filter(Parque::getParParrilla)
                .sorted(Comparator.comparing(Parque::getParNombre))
                .toList();

        if (listaParquesBBQ.isEmpty()) {
            throw new IllegalArgumentException("No existen parques con servicio de parrillas");
        }

        return listaParquesBBQ;
    }
}
