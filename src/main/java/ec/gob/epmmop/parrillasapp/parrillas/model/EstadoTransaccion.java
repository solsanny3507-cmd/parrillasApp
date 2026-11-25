package ec.gob.epmmop.parrillasapp.parrillas.model;

public enum EstadoTransaccion {

    PENDIENTE("PENDIENTE"),
    APROBADO("APROBADO"),
    RECHAZADO("RECHAZADO"),
    ERROR("ERROR");


    private final String descripcion;

    EstadoTransaccion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
