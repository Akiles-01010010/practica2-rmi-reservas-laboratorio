package comun;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * INTERFAZ REMOTA: el "menú" de lo que el cliente puede pedirle al servidor.
 * El cliente y el servidor comparten este archivo (va dentro de comun.jar).
 * Cada método declara RemoteException porque cualquier llamada por red puede fallar.
 */
public interface IReservaService extends Remote {

    /** Nombre con el que el objeto remoto se publica en el registro RMI. */
    String NOMBRE_SERVICIO = "ReservaLaboratorioService";
    int PUERTO_REGISTRO_DEFECTO = 1099;
    int TOTAL_EQUIPOS = 20;
    int FILAS = 4;
    int COLUMNAS = 5;

    /** Estado actual de los 20 equipos (BD + bloqueos temporales en memoria). */
    List<EquipoDTO> obtenerEquipos() throws RemoteException, ReservaException;

    /**
     * Paso 1 de la reserva: pone el equipo EN_PROCESO a nombre del alumno durante
     * unos segundos para que nadie más pueda meterse mientras confirma.
     */
    EquipoDTO iniciarReserva(int idEquipo, String idAlumno) throws RemoteException, ReservaException;

    /**
     * Paso 2: confirma la reserva. Sección crítica protegida con ReentrantLock
     * + transacción JDBC (commit/rollback). Devuelve el comprobante.
     */
    TicketReserva apartarEquipo(ReservaRequest request) throws RemoteException, ReservaException;

    /** El alumno se arrepintió (botón Cancelar / cerró el diálogo): vuelve a DISPONIBLE. */
    void cancelarIntento(int idEquipo, String idAlumno) throws RemoteException;

    /** Cancela una reserva YA confirmada (solo su dueño). */
    void cancelarReserva(int idEquipo, String idAlumno) throws RemoteException, ReservaException;

    /** Recupera el ticket activo de un alumno (por si se perdió la respuesta por la red). */
    TicketReserva consultarTicket(int idEquipo, String idAlumno) throws RemoteException, ReservaException;

    /** Prueba de vida: devuelve la hora del servidor. */
    String ping() throws RemoteException;
}
