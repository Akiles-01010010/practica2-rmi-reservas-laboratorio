package comun;

/**
 * TIMEOUT: la petición se canceló porque algo tardó demasiado:
 *  - la base de datos no respondió (timeout de conexión o de consulta JDBC), o
 *  - el candado (ReentrantLock.tryLock) del equipo no se obtuvo a tiempo.
 * En ambos casos el servidor hace rollback y no deja nada "a medias".
 */
public class ServicioNoDisponibleException extends ReservaException {

    private static final long serialVersionUID = 1L;

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
