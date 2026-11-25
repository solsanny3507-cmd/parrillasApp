package ec.gob.epmmop.parrillasapp.miquito.service;

import ec.gob.epmmop.parrillasapp.miquito.config.MiQuitoConfig;
import ec.gob.epmmop.parrillasapp.miquito.dto.NotificacionMiQuitoRequestDTO;
import ec.gob.epmmop.parrillasapp.miquito.exception.MiQuitoNotificacionException;
import ec.gob.epmmop.parrillasapp.miquito.model.ResultadoNotificacionMiQuito;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class MiQuitoNotificacionService {

    private static final Logger LOGGER =
            Logger.getLogger(
                    MiQuitoNotificacionService.class.getName()
            );

    private static final String TITULO =
            "Reserva de parrillas";

    private static final String DEEPLINK =
            "/buzon";

    @Inject
    private MiQuitoConfig miQuitoConfig;

    private Client client;

    @PostConstruct
    public void init() {

        this.client =
                ClientBuilder
                        .newBuilder()
                        .connectTimeout(
                                miQuitoConfig.getConnectTimeout(),
                                TimeUnit.SECONDS
                        )
                        .readTimeout(
                                miQuitoConfig.getReadTimeout(),
                                TimeUnit.SECONDS
                        )
                        .build();
    }

    @PreDestroy
    public void destroy() {

        if (client != null) {
            client.close();
        }
    }

    public ResultadoNotificacionMiQuito notificarReservaExitosa(
            String identificacion,
            String nombreParque,
            Integer requestId) {

        validarDatos(
                identificacion,
                nombreParque
        );

        String cuerpo =
                "Su reserva de parrilla en Parque "
                + nombreParque
                + " fue realizada exitosamente";

        NotificacionMiQuitoRequestDTO request =
                new NotificacionMiQuitoRequestDTO(
                        identificacion.trim(),
                        TITULO,
                        cuerpo,
                        DEEPLINK
                );

        try (Response response =
                     client
                             .target(
                                     miQuitoConfig
                                             .getNotificacionesUrl()
                             )
                             .request(
                                     MediaType.APPLICATION_JSON_TYPE
                             )
                             .post(
                                     Entity.entity(
                                             request,
                                             MediaType.APPLICATION_JSON_TYPE
                                     )
                             )) {

            int status =
                    response.getStatus();

            if (status == Response.Status.OK
                    .getStatusCode()) {

                LOGGER.info(
                        "MI_QUITO_NOTIFICACION"
                        + " | requestId="
                        + requestId
                        + " | resultado=ENVIADA"
                );

                return ResultadoNotificacionMiQuito.ENVIADA;
            }

            /*
             * De acuerdo con el contrato de Mi Quito,
             * 404 significa que el usuario no tiene
             * dispositivos con sesión activa.
             *
             * No es un error técnico y no se reintenta.
             */
            if (status == Response.Status.NOT_FOUND
                    .getStatusCode()) {

                LOGGER.info(
                        "MI_QUITO_NOTIFICACION"
                        + " | requestId="
                        + requestId
                        + " | resultado=SIN_SESION"
                );

                return ResultadoNotificacionMiQuito.SIN_SESION;
            }

            LOGGER.warning(
                    "MI_QUITO_NOTIFICACION"
                    + " | requestId="
                    + requestId
                    + " | resultado=ERROR"
                    + " | httpStatus="
                    + status
            );

            throw new MiQuitoNotificacionException(
                    "Mi Quito respondió HTTP "
                    + status
            );

        } catch (ProcessingException e) {

            LOGGER.log(
                    Level.SEVERE,
                    "MI_QUITO_NOTIFICACION"
                    + " | requestId="
                    + requestId
                    + " | error de comunicación",
                    e
            );

            throw new MiQuitoNotificacionException(
                    "No fue posible comunicarse con Mi Quito",
                    e
            );
        }
    }

    private void validarDatos(
            String identificacion,
            String nombreParque) {

        if (identificacion == null
            || identificacion.isBlank()) {

            throw new IllegalArgumentException(
                    "La identificación es obligatoria "
                    + "para notificar a Mi Quito"
            );
        }

        if (nombreParque == null
            || nombreParque.isBlank()) {

            throw new IllegalArgumentException(
                    "El nombre del parque es obligatorio "
                    + "para notificar a Mi Quito"
            );
        }
    }
}