package ec.gob.epmmop.parrillasapp.parrillas.exception;

public class ReservaNoDisponibleException
        extends RuntimeException {

    public ReservaNoDisponibleException(
            String message) {

        super(message);
    }

    public ReservaNoDisponibleException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}
