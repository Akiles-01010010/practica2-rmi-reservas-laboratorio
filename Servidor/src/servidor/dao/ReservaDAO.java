package servidor.dao;

import comun.TicketReserva;
import servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** DAO de la tabla reservas (tickets / comprobantes). */
public class ReservaDAO {

    private final ConexionBD bd;

    public ReservaDAO(ConexionBD bd) {
        this.bd = bd;
    }

    /** Inserta el ticket. Se llama DENTRO de la transacción (misma conexión). */
    public void insertar(Connection con, TicketReserva t, String clienteHost) throws SQLException {
        String sql = "INSERT INTO reservas (codigo_confirmacion, id_alumno, id_equipo, hora_inicio, hora_fin, "
                   + "marca_tiempo, firma_sha256, cliente_host, estado) VALUES (?,?,?,?,?,?,?,?,'ACTIVA')";
        try (PreparedStatement ps = bd.preparar(con, sql)) {
            ps.setString(1, t.getCodigoConfirmacion());
            ps.setString(2, t.getIdAlumno());
            ps.setInt(3, t.getIdEquipo());
            ps.setObject(4, t.getHoraInicio());
            ps.setObject(5, t.getHoraFin());
            ps.setObject(6, t.getMarcaTiempo());
            ps.setString(7, t.getFirmaDigital());
            ps.setString(8, clienteHost);
            ps.executeUpdate();
        }
    }

    /** ¿Quién tiene la reserva activa de este equipo? (null si nadie) */
    public String alumnoConReservaActiva(Connection con, int idEquipo) throws SQLException {
        try (PreparedStatement ps = bd.preparar(con, "SELECT id_alumno FROM reservas WHERE equipo_activo = ?")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Ticket activo del equipo (null si no hay). Abre su propia conexión. */
    public TicketReserva buscarActiva(int idEquipo) throws SQLException {
        String sql = "SELECT r.codigo_confirmacion, r.id_alumno, r.id_equipo, e.nombre, r.hora_inicio, r.hora_fin, "
                   + "r.marca_tiempo, r.firma_sha256 FROM reservas r JOIN equipos e ON e.id_equipo = r.id_equipo "
                   + "WHERE r.equipo_activo = ?";
        try (Connection con = bd.abrir(); PreparedStatement ps = bd.preparar(con, sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new TicketReserva(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                        rs.getObject(5, LocalDateTime.class), rs.getObject(6, LocalDateTime.class),
                        rs.getObject(7, LocalDateTime.class), rs.getString(8));
            }
        }
    }

    /** Marca como CANCELADA la reserva activa del equipo si pertenece al alumno. */
    public int cancelarActiva(Connection con, int idEquipo, String idAlumno) throws SQLException {
        String sql = "UPDATE reservas SET estado = 'CANCELADA', cancelada_en = ? "
                   + "WHERE equipo_activo = ? AND id_alumno = ?";
        try (PreparedStatement ps = bd.preparar(con, sql)) {
            ps.setObject(1, LocalDateTime.now());
            ps.setInt(2, idEquipo);
            ps.setString(3, idAlumno);
            return ps.executeUpdate();
        }
    }
}
