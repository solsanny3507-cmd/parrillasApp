package ec.gob.epmmop.parrillasapp.api.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

@ApplicationScoped
public class ApiConfig {

    private static final Logger LOGGER =
            Logger.getLogger(
                    ApiConfig.class.getName()
            );

    private static final String CONFIG_FILE =
            "api.properties";

    private final Properties properties =
            new Properties();

    @PostConstruct
    public void init() {

        try (InputStream inputStream =
                     Thread.currentThread()
                             .getContextClassLoader()
                             .getResourceAsStream(
                                     CONFIG_FILE
                             )) {

            if (inputStream == null) {

                throw new IllegalStateException(
                        "No se encontró el archivo de configuración: "
                        + CONFIG_FILE
                );
            }

            properties.load(
                    inputStream
            );

            validateRequiredProperties();

            LOGGER.info(
                    "Configuración API Mi Quito cargada correctamente"
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible cargar el archivo "
                    + CONFIG_FILE,
                    e
            );
        }
    }

    private void validateRequiredProperties() {

        requireProperty(
                "api.security.token"
        );
    }

    private void requireProperty(
            String key) {

        String value =
                properties.getProperty(
                        key
                );

        if (value == null
            || value.isBlank()) {

            throw new IllegalStateException(
                    "La propiedad obligatoria '"
                    + key
                    + "' no se encuentra configurada"
            );
        }
    }

    public String apiToken() {

        return getRequiredProperty(
                "api.security.token"
        );
    }

    private String getRequiredProperty(
            String key) {

        String value =
                properties.getProperty(
                        key
                );

        if (value == null
            || value.isBlank()) {

            throw new IllegalStateException(
                    "No se encuentra configurada la propiedad: "
                    + key
            );
        }

        return value.trim();
    }
}