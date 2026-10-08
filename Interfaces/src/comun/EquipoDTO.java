package comun;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) con la "foto" de una computadora.
 * Lo envía el servidor al cliente para pintar el plano del laboratorio.
 */
public class EquipoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int idEquipo;
    private String nombre;
    private String especificaciones;
    private EstadoEquipo estado;
    /** Alumno que lo tiene reservado o "en proceso" (null si está libre). */
    private String ocupadoPor;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    /** Solo para EN_PROCESO: segundos que le quedan al bloqueo temporal. */
    private int segundosRestantesBloqueo;

    public EquipoDTO() {
    }

    public EquipoDTO(int idEquipo, String nombre, String especificaciones, EstadoEquipo estado) {
        this.idEquipo = idEquipo;
        this.nombre = nombre;
        this.especificaciones = especificaciones;
        this.estado = estado;
    }

    public int getIdEquipo() { return idEquipo; }
    public void setIdEquipo(int idEquipo) { this.idEquipo = idEquipo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecificaciones() { return especificaciones; }
    public void setEspecificaciones(String especificaciones) { this.especificaciones = especificaciones; }

    public EstadoEquipo getEstado() { return estado; }
    public void setEstado(EstadoEquipo estado) { this.estado = estado; }

    public String getOcupadoPor() { return ocupadoPor; }
    public void setOcupadoPor(String ocupadoPor) { this.ocupadoPor = ocupadoPor; }

    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }

    public int getSegundosRestantesBloqueo() { return segundosRestantesBloqueo; }
    public void setSegundosRestantesBloqueo(int s) { this.segundosRestantesBloqueo = s; }

    @Override
    public String toString() {
        return nombre + " [" + estado + (ocupadoPor != null ? " por " + ocupadoPor : "") + "]";
    }
}
