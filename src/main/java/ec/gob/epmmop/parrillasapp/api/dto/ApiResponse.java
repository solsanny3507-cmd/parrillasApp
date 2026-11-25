package ec.gob.epmmop.parrillasapp.api.dto;

import jakarta.json.bind.annotation.JsonbPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonbPropertyOrder({
        "status",
        "success",
        "code",
        "message",
        "data",
        "timestamp"
})
public class ApiResponse<T> {

    private static final ZoneId ZONA_ECUADOR =
            ZoneId.of("America/Guayaquil");

    private int status;
    private boolean success;
    private String code;
    private String message;
    private T data;

    /*
     * Se maneja como String para evitar dependencia de módulos
     * adicionales de Jackson para java.time.
     *
     * Formato ISO-8601:
     * 2026-10-13T21:05:33.123Z
     */
    private String timestamp;

    public static <T> ApiResponse<T> success(
            int status,
            String code,
            String message,
            T data) {

        return new ApiResponse<>(
                status,
                true,
                code,
                message,
                data,
                timestampActual()
        );
    }

    public static <T> ApiResponse<T> error(
            int status,
            String code,
            String message) {

        return new ApiResponse<>(
                status,
                false,
                code,
                message,
                null,
                timestampActual()
        );
    }

    private static String timestampActual() {

        return OffsetDateTime
                .now(ZONA_ECUADOR)
                .toString();
    }
}