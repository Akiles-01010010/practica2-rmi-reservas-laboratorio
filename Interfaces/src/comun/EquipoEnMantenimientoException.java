package comun;

/** El equipo no está disponible funcionalmente (está en mantenimiento). */
public class EquipoEnMantenimientoException extends ReservaException {

    private static final long serialVersionUID = 1L;

    private final int idEquipo;

    public EquipoEnMantenimientoException(int idEquipo) {
        super("El equipo PC-" + String.format("%02d", idEquipo)
                + " está EN MANTENIMIENTO y no se puede reservar.");
        this.idEquipo = idEquipo;
    }

    public int getIdEquipo() { return idEquipo; }
}
