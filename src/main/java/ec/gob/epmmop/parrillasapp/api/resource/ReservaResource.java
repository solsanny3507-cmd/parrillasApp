package ec.gob.epmmop.parrillasapp.api.resource;

import ec.gob.epmmop.parrillasapp.api.dto.ApiResponse;
import ec.gob.epmmop.parrillasapp.api.dto.ReservaActivaRequestDTO;
import ec.gob.epmmop.parrillasapp.api.dto.ReservaActivaResponseDTO;
import ec.gob.epmmop.parrillasapp.api.security.Secured;
import ec.gob.epmmop.parrillasapp.api.service.ReservaConsultaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("/reservas")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Secured
public class ReservaResource {

    private static final Logger LOGGER =
            Logger.getLogger(
                    ReservaResource.class.getName()
            );

    @Inject
    private ReservaConsultaService reservaConsultaService;

    @POST
    @Path("/activas")
    public Response consultarReservasActivas(
            ReservaActivaRequestDTO request) {

        /*
         * Validamos únicamente presencia.
         *
         * No restringimos a 10 dígitos porque el modelo
         * almacena la identificación como String y podemos
         * soportar otros tipos en el futuro.
         */
        if (request == null
            || request.getIdentificacion() == null
            || request.getIdentificacion().isBlank()) {

            ApiResponse<Void> response =
                    ApiResponse.error(
                            Response.Status.BAD_REQUEST
                                    .getStatusCode(),
                            "IDENTIFICATION_REQUIRED",
                            "La identificación es obligatoria"
                    );

            return Response
                    .status(
                            Response.Status.BAD_REQUEST
                    )
                    .entity(response)
                    .build();
        }

        ReservaActivaResponseDTO data =
                reservaConsultaService
                        .consultarReservasActivas(
                                request.getIdentificacion()
                        );

        /*
         * Consulta válida pero sin reservas.
         *
         * No utilizamos 404 porque el recurso de consulta
         * sí fue procesado correctamente.
         */
        if (data.getCantidad() == 0) {

            LOGGER.info(
                    "MI_QUITO_RESERVAS_CONSULTA"
                    + " | resultado=SIN_RESERVAS"
            );

            ApiResponse<ReservaActivaResponseDTO> response =
                    ApiResponse.success(
                            Response.Status.OK
                                    .getStatusCode(),
                            "NO_ACTIVE_RESERVATIONS",
                            "El ciudadano no registra reservas activas",
                            data
                    );

            return Response
                    .ok(response)
                    .build();
        }

        LOGGER.info(
                "MI_QUITO_RESERVAS_CONSULTA"
                + " | resultado=OK"
                + " | cantidad="
                + data.getCantidad()
        );

        ApiResponse<ReservaActivaResponseDTO> response =
                ApiResponse.success(
                        Response.Status.OK
                                .getStatusCode(),
                        "ACTIVE_RESERVATIONS_FOUND",
                        "Consulta realizada correctamente",
                        data
                );

        return Response
                .ok(response)
                .build();
    }
}