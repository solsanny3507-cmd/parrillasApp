package ec.gob.epmmop.parrillasapp.api.security;

import ec.gob.epmmop.parrillasapp.api.config.ApiConfig;
import ec.gob.epmmop.parrillasapp.api.dto.ApiResponse;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Logger;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class ApiTokenFilter
        implements ContainerRequestFilter {

    private static final Logger LOGGER =
            Logger.getLogger(
                    ApiTokenFilter.class.getName()
            );

    private static final String BEARER_PREFIX =
            "Bearer ";

    @Inject
    private ApiConfig config;

    @Override
    public void filter(
            ContainerRequestContext requestContext) {

        String authorizationHeader =
                requestContext.getHeaderString(
                        HttpHeaders.AUTHORIZATION
                );

        /*
         * No se recibió Authorization.
         */
        if (authorizationHeader == null
            || authorizationHeader.isBlank()) {

            LOGGER.warning(
                    "MI_QUITO_API | solicitud rechazada"
                    + " | motivo=TOKEN_NO_ENVIADO"
            );

            abortUnauthorized(
                    requestContext,
                    "AUTH_TOKEN_REQUIRED",
                    "Token de autenticación requerido"
            );

            return;
        }

        /*
         * Verificamos esquema Bearer.
         */
        if (!authorizationHeader.regionMatches(
                true,
                0,
                BEARER_PREFIX,
                0,
                BEARER_PREFIX.length())) {

            LOGGER.warning(
                    "MI_QUITO_API | solicitud rechazada"
                    + " | motivo=ESQUEMA_INVALIDO"
            );

            abortUnauthorized(
                    requestContext,
                    "INVALID_AUTH_SCHEME",
                    "Se requiere autenticación Bearer"
            );

            return;
        }

        String token =
                authorizationHeader
                        .substring(
                                BEARER_PREFIX.length()
                        )
                        .trim();

        /*
         * Authorization: Bearer
         * pero sin contenido.
         */
        if (token.isBlank()) {

            LOGGER.warning(
                    "MI_QUITO_API | solicitud rechazada"
                    + " | motivo=TOKEN_VACIO"
            );

            abortUnauthorized(
                    requestContext,
                    "AUTH_TOKEN_REQUIRED",
                    "Token de autenticación requerido"
            );

            return;
        }

        String configuredToken =
                config.apiToken();

        /*
         * Comparación segura.
         *
         * Nunca registramos el token recibido
         * ni el token configurado.
         */
        if (!secureEquals(
                token,
                configuredToken)) {

            LOGGER.warning(
                    "MI_QUITO_API | solicitud rechazada"
                    + " | motivo=TOKEN_INVALIDO"
            );

            abortUnauthorized(
                    requestContext,
                    "INVALID_TOKEN",
                    "Token de autenticación inválido"
            );

            return;
        }

        LOGGER.fine(
                "MI_QUITO_API | Bearer Token válido"
        );
    }

    private boolean secureEquals(
            String receivedToken,
            String configuredToken) {

        byte[] received =
                receivedToken.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] configured =
                configuredToken.getBytes(
                        StandardCharsets.UTF_8
                );

        return MessageDigest.isEqual(
                received,
                configured
        );
    }

    private void abortUnauthorized(
            ContainerRequestContext requestContext,
            String code,
            String message) {

        ApiResponse<Void> response =
                ApiResponse.error(
                        Response.Status.UNAUTHORIZED
                                .getStatusCode(),
                        code,
                        message
                );

        requestContext.abortWith(
                Response
                        .status(
                                Response.Status.UNAUTHORIZED
                        )
                        .entity(response)
                        .type(
                                MediaType.APPLICATION_JSON
                        )
                        .build()
        );
    }
}