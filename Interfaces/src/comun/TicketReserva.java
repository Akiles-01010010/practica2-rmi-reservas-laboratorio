package comun;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO que el SERVIDOR devuelve cuando la reserva se confirmó.
 * Es el "comprobante con sello": código de confirmación, marca de tiempo
 * y una firma digital SIMULADA (hash SHA-256 de los datos + un secreto
 * que solo conoce el servidor).
 */
public class TicketReserva implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String codigoConfirmacion;
    private final String idAlumno;
    private final int idEquipo;
    private final String nombreEquipo;
    private final LocalDateTime horaInicio;
    private final LocalDateTime horaFin;
    private final LocalDateTime marcaTiempo;
    private final String firmaDigital;

    public TicketReserva(String codigoConfirmacion, String idAlumno, int idEquipo, String nombreEquipo,
                         LocalDateTime horaInicio, LocalDateTime horaFin,
                         LocalDateTime marcaTiempo, String firmaDigital) {
        this.codigoConfirmacion = codigoConfirmacion;
        this.idAlumno = idAlumno;
        this.idEquipo = idEquipo;
        this.nombreEquipo = nombreEquipo;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.marcaTiempo = marcaTiempo;
        this.firmaDigital = firmaDigital;
    }

    /**
     * Texto "canónico" (siempre en el mismo orden) con el que se calcula la firma.
     * Si alguien cambiara un solo dato del ticket, el hash ya no coincidiría.
     */
    public static String datosCanonicos(String codigo, String idAlumno, int idEquipo,
                                        LocalDateTime inicio, LocalDateTime fin, LocalDateTime marca) {
        return codigo + "|" + idAlumno + "|" + idEquipo + "|" + inicio + "|" + fin + "|" + marca;
    }

    public String getCodigoConfirmacion() { return codigoConfirmacion; }
    public String getIdAlumno() { return idAlumno; }
    public int getIdEquipo() { return idEquipo; }
    public String getNombreEquipo() { return nombreEquipo; }
    public LocalDateTime getHoraInicio() { return horaInicio; }
    public LocalDateTime getHoraFin() { return horaFin; }
    public LocalDateTime getMarcaTiempo() { return marcaTiempo; }
    public String getFirmaDigital() { return firmaDigital; }

    public String getMarcaTiempoTexto() {
        return marcaTiempo == null ? "" : marcaTiempo.format(FMT);
    }

    @Override
    public String toString() {
        return "Ticket " + codigoConfirmacion + " | alumno " + idAlumno + " | " + nombreEquipo
                + " | " + horaInicio.toLocalTime() + "-" + horaFin.toLocalTime()
                + " | " + getMarcaTiempoTexto() + " | firma " + firmaDigital;
    }
}
