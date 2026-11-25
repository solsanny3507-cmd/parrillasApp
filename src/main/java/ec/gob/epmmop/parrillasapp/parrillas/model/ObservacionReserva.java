package ec.gob.epmmop.parrillasapp.parrillas.model;

public enum ObservacionReserva {

    PENDIENTE("Pendiente de pago"),
    PAGADO("Reserva exitosa"),
    CANCELADO("Reserva rechazada o cancelada"),
    EXPIRADO("Reserva expirada");

    private final String descripcion;

    ObservacionReserva(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
