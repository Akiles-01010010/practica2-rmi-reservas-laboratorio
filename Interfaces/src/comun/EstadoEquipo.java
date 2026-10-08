package comun;

/**
 * Estados posibles de una computadora del laboratorio.
 * Los enum de Java ya son Serializable, por eso pueden viajar por RMI.
 *
 *  - DISPONIBLE    : verde, cualquiera puede apartarla.
 *  - EN_PROCESO    : amarillo, alguien abrió el diálogo de reserva y la tiene
 *                    "apartada temporalmente" (vive solo en memoria del servidor).
 *  - RESERVADO     : rojo, ya tiene un ticket confirmado en la base de datos.
 *  - MANTENIMIENTO : gris, no se puede usar.
 */
public enum EstadoEquipo {
    DISPONIBLE("Disponible"),
    EN_PROCESO("En proceso"),
    RESERVADO("Reservado"),
    MANTENIMIENTO("Mantenimiento");

    private final String etiqueta;

    EstadoEquipo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
