package comun;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO que el CLIENTE envía al servidor para pedir un equipo.
 * Es como llenar el "formato de solicitud" en papel y meterlo al buzón:
 * RMI lo convierte en bytes (serialización), viaja por la red y el servidor
 * lo reconstruye idéntico (deserialización).
 */
public class ReservaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String idAlumno;
    private final int idEquipo;
    private final LocalDateTime horaInicio;
    private final LocalDateTime horaFin;

    public ReservaRequest(String idAlumno, int idEquipo, LocalDateTime horaInicio, LocalDateTime horaFin) {
        this.idAlumno = idAlumno;
        this.idEquipo = idEquipo;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public String getIdAlumno() { return idAlumno; }
    public int getIdEquipo() { return idEquipo; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public LocalDateTime getHoraFin() { return horaFin; }

    @Override
    public String toString() {
        return "ReservaRequest{alumno=" + idAlumno + ", equipo=" + idEquipo
                + ", inicio=" + horaInicio + ", fin=" + horaFin + "}";
    }
}
