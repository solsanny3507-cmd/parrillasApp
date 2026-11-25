package ec.gob.epmmop.parrillasapp.placetopay.config;

public class PagoException extends RuntimeException{

    public PagoException(String mensaje) {
        super(mensaje);
    }

    public PagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
