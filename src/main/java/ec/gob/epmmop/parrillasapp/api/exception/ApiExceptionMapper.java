package ec.gob.epmmop.parrillasapp.api.exception;

import ec.gob.epmmop.parrillasapp.api.dto.ApiResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ApiExceptionMapper
        implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(
                    ApiExceptionMapper.class.getName()
            );

    @Override
    public Response toResponse(
            Throwable exception) {

        /*
         * Errores HTTP producidos por JAX-RS.
         */
        if (exception instanceof WebApplicationException webException) {

            return procesarWebApplicationException(
                    webException
            );
        }

        /*
         * Error inesperado.
         *
         * El detalle completo queda exclusivamente
         * en server.log.
         */
        LOGGER.log(
                Level.SEVERE,
                "MI_QUITO_API | error interno no controlado",
                exception
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        Response.Status.INTERNAL_SERVER_ERROR
                                .getStatusCode(),
                        "INTERNAL_ERROR",
                        "No fue posible procesar la consulta"
                );

        return Response
                .status(
                        Response.Status.INTERNAL_SERVER_ERROR
                )
                .type(
                        MediaType.APPLICATION_JSON
                )
                .entity(response)
                .build();
    }

    private Response procesarWebApplicationException(
            WebApplicationException exception) {

        int status =
                exception
                        .getResponse()
                        .getStatus();

        String code;
        String message;

        if (exception instanceof BadRequestException) {

            code =
                    "INVALID_REQUEST";

            message =
                    "La solicitud enviada no es válida";

        } else if (exception instanceof NotSupportedException) {

            code =
                    "UNSUPPORTED_MEDIA_TYPE";

            message =
                    "El formato de contenido enviado no es soportado";

        } else if (exception instanceof NotAllowedException) {

            code =
                    "METHOD_NOT_ALLOWED";

            message =
                    "El método HTTP utilizado no está permitido";

        } else if (exception instanceof NotFoundException) {

            code =
                    "RESOURCE_NOT_FOUND";

            message =
                    "El recurso solicitado no existe";

        } else {

            code =
                    "HTTP_ERROR";

            message =
                    "No fue posible procesar la solicitud";
        }

        ApiResponse<Void> response =
                ApiResponse.error(
                        status,
                        code,
                        message
                );

        return Response
                .status(status)
                .type(
                        MediaType.APPLICATION_JSON
                )
                .entity(response)
                .build();
    }
}