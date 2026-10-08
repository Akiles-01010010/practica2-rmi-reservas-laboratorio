package servidor.dao;

import servidor.Consola;
import servidor.bd.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DAO de la bitácora.
 *  - registrar(con, ...)      : dentro de una transacción (se confirma o revierte junto con ella).
 *  - registrarAsync(...)      : para eventos sueltos (rechazos, expiraciones). Se escribe en un
 *                               hilo aparte para NO alargar la sección crítica ni bloquear al
 *                               cliente si la BD está lenta.
 */
public class BitacoraDAO {

    private static final String SQL = "INSERT INTO bitacora (fecha, id_alumno, id_equipo, accion, resultado, detalle, cliente_host) "
                                    + "VALUES (?,?,?,?,?,?,?)";

    private final ConexionBD bd;
    private final ExecutorService escritor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "bitacora");
        t.setDaemon(true);
        return t;
    });

    public BitacoraDAO(ConexionBD bd) {
        this.bd = bd;
    }

    public void registrar(Connection con, String idAlumno, Integer idEquipo, String accion,
                          String resultado, String detalle, String host) throws SQLException {
        try (PreparedStatement ps = bd.preparar(con, SQL)) {
            ps.setObject(1, LocalDateTime.now());
            ps.setString(2, idAlumno);
            if (idEquipo == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, idEquipo);
            ps.setString(4, accion);
            ps.setString(5, resultado);
            ps.setString(6, detalle == null ? null : (detalle.length() > 255 ? detalle.substring(0, 255) : detalle));
            ps.setString(7, host);
            ps.executeUpdate();
        }
    }

    public void registrarAsync(String idAlumno, Integer idEquipo, String accion,
                               String resultado, String detalle, String host) {
        escritor.submit(() -> {
            try (Connection con = bd.abrir()) {
                registrar(con, idAlumno, idEquipo, accion, resultado, detalle, host);
            } catch (SQLException e) {
                Consola.aviso("No se pudo escribir en bitácora (" + accion + "): " + e.getMessage());
            }
        });
    }

    public void cerrar() {
        escritor.shutdown();
        try {
            escritor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
