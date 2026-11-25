package ec.gob.epmmop.parrillasapp.parrillas.model;

public enum EstadoReserva {

    PENDIENTE("PENDIENTE"),
    RESERVADA("RESERVADO"),
    CANCELADA("CANCELADO"),
    EXPIRADA("EXPIRADA");

    private final String descripcion;

    EstadoReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
