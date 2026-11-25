package ec.gob.epmmop.parrillasapp.miquito.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class MiQuitoConfig {

    private static final String ARCHIVO =
            "miquito.properties";

    private final Properties properties =
            new Properties();

    @PostConstruct
    public void init() {

        try (InputStream input =
                     Thread.currentThread()
                             .getContextClassLoader()
                             .getResourceAsStream(ARCHIVO)) {

            if (input == null) {
                throw new IllegalStateException(
                        "No se encontró " + ARCHIVO
                );
            }

            properties.load(input);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "No fue posible cargar "
                    + ARCHIVO,
                    e
            );
        }
    }

    public String getNotificacionesUrl() {

        return obtenerObligatorio(
                "miquito.notificaciones.url"
        );
    }

    public long getConnectTimeout() {

        return Long.parseLong(
                properties.getProperty(
                        "miquito.notificaciones.connect-timeout",
                        "5"
                )
        );
    }

    public long getReadTimeout() {

        return Long.parseLong(
                properties.getProperty(
                        "miquito.notificaciones.read-timeout",
                        "10"
                )
        );
    }

    private String obtenerObligatorio(String key) {

        String valor = properties.getProperty(key);

        if (valor == null || valor.isBlank()) {

            throw new IllegalStateException(
                    "Propiedad requerida no configurada: "
                    + key
            );
        }

        return valor.trim();
    }
}