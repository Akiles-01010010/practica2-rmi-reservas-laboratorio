package comun;

/**
 * (Extra solicitado) Otro alumno tiene el equipo en estado EN_PROCESO:
 * abrió el diálogo de reserva y todavía no confirma ni cancela.
 * Cuando confirme, cancele o se le acabe el tiempo, el equipo se libera.
 */
public class EquipoEnProcesoException extends ReservaException {

    private static final long serialVersionUID = 1L;

    private final int idEquipo;
    private final String alumno;
    private final int segundosRestantes;

    public EquipoEnProcesoException(int idEquipo, String alumno, int segundosRestantes) {
        super("El alumno " + alumno + " está reservando el equipo PC-" + String.format("%02d", idEquipo)
                + " en este momento. Se liberará en máximo " + segundosRestantes
                + " s si no confirma. Intenta de nuevo en unos segundos o elige otro equipo.");
        this.idEquipo = idEquipo;
        this.alumno = alumno;
        this.segundosRestantes = segundosRestantes;
    }

    public int getIdEquipo() { return idEquipo; }
    public String getAlumno() { return alumno; }
    public int getSegundosRestantes() { return segundosRestantes; }
}
