package servidor.bd;

import comun.ReservaException;
import comun.ServicioNoDisponibleException;
import servidor.Configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;

/**
 * Fábrica de conexiones JDBC a MySQL con TODOS los timeouts configurados,
 * para que ninguna petición se quede colgada si la base de datos no responde:
 *
 *  1. DriverManager.setLoginTimeout  -> máximo para obtener la conexión.
 *  2. connectTimeout (en la URL)     -> máximo para abrir el socket TCP.
 *  3. socketTimeout  (en la URL)     -> máximo esperando bytes del servidor MySQL.
 *  4. Statement.setQueryTimeout      -> máximo por consulta (MySQL la aborta).
 *  5. innodb_lock_wait_timeout       -> máximo esperando una fila bloqueada.
 *
 * Se abre una conexión nueva por operación (sin pool) para que el ejemplo sea
 * sencillo y cada transacción sea independiente; al cerrarla, si algo quedó
 * sin commit, MySQL hace rollback automáticamente.
 */
public class ConexionBD {

    private final String url;
    private final String usuario;
    private final String password;
    private final int queryTimeoutSeg;
    private final int lockWaitTimeoutSeg;

    public ConexionBD(Configuracion cfg) {
        this.url = cfg.getDbUrl();
        this.usuario = cfg.getDbUsuario();
        this.password = cfg.getDbPassword();
        this.queryTimeoutSeg = cfg.getDbQueryTimeoutSeg();
        this.lockWaitTimeoutSeg = cfg.getDbLockWaitTimeoutSeg();
        DriverManager.setLoginTimeout(cfg.getDbLoginTimeoutSeg());
    }

    /** Verifica que el driver esté en el classpath (si no: ClassNotFoundException). */
    public static void verificarDriver() throws ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
    }

    public Connection abrir() throws SQLException {
        Connection con = DriverManager.getConnection(url, usuario, password);
        try (Statement st = con.createStatement()) {
            st.setQueryTimeout(queryTimeoutSeg);
            st.execute("SET SESSION innodb_lock_wait_timeout = " + lockWaitTimeoutSeg);
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }

    /** PreparedStatement con timeout de consulta ya configurado. */
    public PreparedStatement preparar(Connection con, String sql) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setQueryTimeout(queryTimeoutSeg);
        return ps;
    }

    public static void rollbackSilencioso(Connection con) {
        if (con == null) return;
        try {
            if (!con.isClosed() && !con.getAutoCommit()) {
                con.rollback();
            }
        } catch (SQLException ignorada) {
            // Si la conexión ya murió, MySQL descarta la transacción al detectar el cierre.
        }
    }

    public static void cerrarSilencioso(Connection con) {
        if (con == null) return;
        try {
            if (!con.isClosed()) {
                con.setAutoCommit(true);
                con.close();
            }
        } catch (SQLException ignorada) {
            try { con.close(); } catch (SQLException ignorada2) { /* nada más que hacer */ }
        }
    }

    /** ¿El error es de red/timeout (BD caída o lenta) y no de lógica SQL? */
    public static boolean esFallaDeConexionOTimeout(SQLException e) {
        if (e instanceof SQLTimeoutException
                || e instanceof SQLTransientConnectionException
                || e instanceof SQLNonTransientConnectionException
                || e instanceof SQLRecoverableException) {
            return true;
        }
        String estado = e.getSQLState();
        int codigo = e.getErrorCode();
        return (estado != null && (estado.startsWith("08") || "HYT00".equals(estado) || "HY008".equals(estado)))
                || codigo == 1205   // Lock wait timeout exceeded
                || codigo == 3024;  // Query execution was interrupted (max_execution_time)
    }

    /**
     * Convierte una SQLException (clase del driver, que el cliente NO conoce)
     * en una excepción de negocio que sí viaja bien por RMI.
     */
    public static ReservaException traducir(SQLException e, String operacion) {
        if (esFallaDeConexionOTimeout(e)) {
            return new ServicioNoDisponibleException("TIMEOUT: la base de datos no respondió a tiempo al "
                    + operacion + ". La operación se canceló y se revirtió (rollback); no quedó nada a medias. "
                    + "Intenta de nuevo en unos segundos.");
        }
        return new ReservaException("Error de base de datos al " + operacion + ": " + e.getMessage());
    }
}
