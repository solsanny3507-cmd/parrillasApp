package ec.gob.epmmop.parrillasapp.miquito.exception;

public class MiQuitoNotificacionException extends RuntimeException {

    public MiQuitoNotificacionException(String message) {
        super(message);
    }

    public MiQuitoNotificacionException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}