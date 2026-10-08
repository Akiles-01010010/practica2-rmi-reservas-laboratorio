package comun;

/**
 * El equipo ya fue reservado (ticket confirmado) por otro usuario,
 * normalmente "segundos antes" que tú.
 */
public class ReservaDuplicadaException extends ReservaException {

    private static final long serialVersionUID = 1L;

    private final int idEquipo;
    private final String reservadoPor;

    public ReservaDuplicadaException(int idEquipo, String reservadoPor) {
        super("El equipo PC-" + String.format("%02d", idEquipo) + " ya fue reservado"
                + (reservadoPor != null ? " por el alumno " + reservadoPor : " por otro usuario")
                + " segundos antes. Elige otro equipo.");
        this.idEquipo = idEquipo;
        this.reservadoPor = reservadoPor;
    }

    public int getIdEquipo() { return idEquipo; }
    public String getReservadoPor() { return reservadoPor; }
}
