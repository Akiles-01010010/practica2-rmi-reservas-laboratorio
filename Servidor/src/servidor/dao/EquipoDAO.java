package servidor.dao;

import comun.EquipoDTO;
import comun.EstadoEquipo;
import servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** DAO de la tabla equipos (inventario). */
public class EquipoDAO {

    private static final String SQL_BASE =
            "SELECT e.id_equipo, e.nombre, e.especificaciones, e.estado, "
          + "       r.id_alumno, r.hora_inicio, r.hora_fin "
          + "FROM equipos e "
          + "LEFT JOIN reservas r ON r.equipo_activo = e.id_equipo ";

    private final ConexionBD bd;

    public EquipoDAO(ConexionBD bd) {
        this.bd = bd;
    }

    /** Lista los 20 equipos con su reserva activa (si tienen). */
    public List<EquipoDTO> listar() throws SQLException {
        List<EquipoDTO> lista = new ArrayList<>();
        try (Connection con = bd.abrir();
             PreparedStatement ps = bd.preparar(con, SQL_BASE + "ORDER BY e.id_equipo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Busca un equipo (lectura simple, sin bloquear la fila). */
    public EquipoDTO buscar(int idEquipo) throws SQLException {
        try (Connection con = bd.abrir();
             PreparedStatement ps = bd.preparar(con, SQL_BASE + "WHERE e.id_equipo = ?")) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Dentro de una transacción: lee el equipo y BLOQUEA su fila (SELECT ... FOR UPDATE).
     * Otra transacción que quiera la misma fila tendrá que esperar al commit/rollback.
     */
    public EquipoDTO bloquearFila(Connection con, int idEquipo) throws SQLException {
        String sql = "SELECT id_equipo, nombre, especificaciones, estado FROM equipos WHERE id_equipo = ? FOR UPDATE";
        try (PreparedStatement ps = bd.preparar(con, sql)) {
            ps.setInt(1, idEquipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new EquipoDTO(rs.getInt("id_equipo"), rs.getString("nombre"),
                        rs.getString("especificaciones"), EstadoEquipo.valueOf(rs.getString("estado")));
            }
        }
    }

    /**
     * Cambia el estado SOLO si sigue siendo el esperado ("compare-and-set").
     * Devuelve el número de filas cambiadas (1 = éxito, 0 = alguien lo cambió antes).
     */
    public int cambiarEstado(Connection con, int idEquipo, EstadoEquipo esperado, EstadoEquipo nuevo) throws SQLException {
        String sql = "UPDATE equipos SET estado = ? WHERE id_equipo = ? AND estado = ?";
        try (PreparedStatement ps = bd.preparar(con, sql)) {
            ps.setString(1, nuevo.name());
            ps.setInt(2, idEquipo);
            ps.setString(3, esperado.name());
            return ps.executeUpdate();
        }
    }

    private EquipoDTO mapear(ResultSet rs) throws SQLException {
        EquipoDTO dto = new EquipoDTO(rs.getInt("id_equipo"), rs.getString("nombre"),
                rs.getString("especificaciones"), EstadoEquipo.valueOf(rs.getString("estado")));
        String alumno = rs.getString("id_alumno");
        if (alumno != null) {
            dto.setOcupadoPor(alumno);
            dto.setHoraInicio(rs.getObject("hora_inicio", LocalDateTime.class));
            dto.setHoraFin(rs.getObject("hora_fin", LocalDateTime.class));
        }
        return dto;
    }
}
