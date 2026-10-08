package servidor;

import comun.EquipoDTO;
import comun.EquipoEnMantenimientoException;
import comun.EquipoEnProcesoException;
import comun.EstadoEquipo;
import comun.IReservaService;
import comun.ReservaDuplicadaException;
import comun.ReservaException;
import comun.ReservaRequest;
import comun.ServicioNoDisponibleException;
import comun.TicketReserva;
import servidor.bd.ConexionBD;
import servidor.bd.TransaccionReserva;
import servidor.dao.BitacoraDAO;
import servidor.dao.EquipoDAO;
import servidor.dao.ReservaDAO;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CAPA 2 - OBJETO REMOTO con la lógica y el CONTROL CONCURRENTE.
 *
 * RMI atiende cada llamada de cliente en un HILO distinto. Si dos alumnos piden
 * la PC-07 en el mismo milisegundo, dos hilos entran aquí a la vez: eso es la
 * condición de carrera. La resolvemos con UN ReentrantLock POR EQUIPO:
 *
 *  - ¿Por qué ReentrantLock y no "synchronized"?
 *      1) tryLock(5, SECONDS): si el candado no se obtiene a tiempo, cancelamos la
 *         petición (requisito de TIMEOUT). Con synchronized el hilo esperaría
 *         para siempre, sin forma de rendirse.
 *      2) Un candado por equipo: reservar la PC-03 no hace esperar a quien reserva
 *         la PC-15 (con un método synchronized TODO el servidor quedaría en fila).
 *      3) new ReentrantLock(true) es "justo" (fair): atiende en orden de llegada.
 *      4) Se libera SIEMPRE en finally, aunque haya excepción.
 *
 *  - Doble seguro en la BD: SELECT ... FOR UPDATE + UPDATE condicionado +
 *    UNIQUE de una reserva activa por equipo. Aunque hubiera dos servidores,
 *    MySQL no dejaría guardar dos reservas activas del mismo equipo.
 *
 *  - Estado EN_PROCESO (bloqueo temporal): al abrir el diálogo, el equipo queda
 *    apartado a nombre del alumno N segundos. Se libera al confirmar, al cancelar
 *    o, si el cliente se desconecta, cuando el "limpiador" ve que venció.
 */
public class ReservaServiceImpl extends UnicastRemoteObject implements IReservaService {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final transient Configuracion cfg;
    private final transient EquipoDAO equipoDAO;
    private final transient ReservaDAO reservaDAO;
    private final transient BitacoraDAO bitacoraDAO;
    private final transient TransaccionReserva transaccion;

    /** candados[1..20]: un ReentrantLock "justo" por cada computadora. */
    private final transient ReentrantLock[] candados = new ReentrantLock[TOTAL_EQUIPOS + 1];
    /** Equipos EN_PROCESO (idEquipo -> quién y hasta cuándo). */
    private final transient Map<Integer, BloqueoTemporal> bloqueos = new ConcurrentHashMap<>();
    private final transient ScheduledExecutorService limpiador;

    public ReservaServiceImpl(Configuracion cfg, ConexionBD bd) throws RemoteException {
        super(cfg.getPuertoObjeto()); // exporta el objeto en un PUERTO FIJO (para el firewall)
        this.cfg = cfg;
        this.equipoDAO = new EquipoDAO(bd);
        this.reservaDAO = new ReservaDAO(bd);
        this.bitacoraDAO = new BitacoraDAO(bd);
        this.transaccion = new TransaccionReserva(bd, equipoDAO, reservaDAO, bitacoraDAO, cfg);
        for (int i = 1; i <= TOTAL_EQUIPOS; i++) {
            candados[i] = new ReentrantLock(true);
        }
        limpiador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "limpiador-bloqueos");
            t.setDaemon(true);
            return t;
        });
        limpiador.scheduleAtFixedRate(this::liberarBloqueosVencidos, 2, 2, TimeUnit.SECONDS);
    }

    // =====================================================================
    //  MÉTODOS REMOTOS
    // =====================================================================

    @Override
    public List<EquipoDTO> obtenerEquipos() throws ReservaException {
        try {
            List<EquipoDTO> lista = equipoDAO.listar();
            for (EquipoDTO e : lista) {
                aplicarBloqueoTemporal(e);
            }
            return new ArrayList<>(lista); // ArrayList es Serializable
        } catch (SQLException e) {
            throw ConexionBD.traducir(e, "consultar el inventario");
        }
    }

    @Override
    public EquipoDTO iniciarReserva(int idEquipo, String idAlumno) throws ReservaException {
        String alumno = validarAlumno(idAlumno);
        validarEquipo(idEquipo);
        String host = hostCliente();
        ReentrantLock candado = tomarCandado(idEquipo, alumno, host, "INICIO_RESERVA");
        try {
            // ---------------- SECCIÓN CRÍTICA (un hilo a la vez por equipo) ----------------
            BloqueoTemporal actual = bloqueoVigente(idEquipo);
            if (actual != null && !actual.getIdAlumno().equals(alumno)) {
                throw new EquipoEnProcesoException(idEquipo, actual.getIdAlumno(), actual.segundosRestantes());
            }
            EquipoDTO equipo = buscarEquipo(idEquipo);
            if (equipo.getEstado() == EstadoEquipo.MANTENIMIENTO) {
                throw new EquipoEnMantenimientoException(idEquipo);
            }
            if (equipo.getEstado() == EstadoEquipo.RESERVADO) {
                throw new ReservaDuplicadaException(idEquipo, equipo.getOcupadoPor());
            }
            BloqueoTemporal nuevo = new BloqueoTemporal(alumno, host, cfg.getSegundosBloqueoTemporal());
            bloqueos.put(idEquipo, nuevo);
            equipo.setEstado(EstadoEquipo.EN_PROCESO);
            equipo.setOcupadoPor(alumno);
            equipo.setSegundosRestantesBloqueo(nuevo.segundosRestantes());
            Consola.info("[EN_PROCESO] " + equipo.getNombre() + " apartado temporalmente por " + alumno
                    + " (" + host + ") durante " + cfg.getSegundosBloqueoTemporal() + " s");
            bitacoraDAO.registrarAsync(alumno, idEquipo, "INICIO_RESERVA", "EXITO",
                    "Equipo EN_PROCESO por " + cfg.getSegundosBloqueoTemporal() + " s", host);
            return equipo;
            // -------------------------------------------------------------------------------
        } catch (ReservaException e) {
            registrarRechazo(alumno, idEquipo, "INICIO_RESERVA", e, host);
            throw e;
        } finally {
            candado.unlock();
        }
    }

    /**
     * Equivale a "public synchronized TicketReserva apartarEquipo(ReservaRequest r)",
     * pero con un ReentrantLock por equipo y con tiempo límite (tryLock).
     */
    @Override
    public TicketReserva apartarEquipo(ReservaRequest r) throws ReservaException {
        if (r == null) {
            throw new ReservaException("La solicitud de reserva llegó vacía.");
        }
        final String alumno = validarAlumno(r.getIdAlumno());
        final int idEquipo = r.getIdEquipo();
        validarEquipo(idEquipo);
        validarHorario(r);
        String host = hostCliente();
        long inicio = System.nanoTime();

        ReentrantLock candado = tomarCandado(idEquipo, alumno, host, "RESERVA");
        try {
            // ---------------- SECCIÓN CRÍTICA (un hilo a la vez por equipo) ----------------
            BloqueoTemporal actual = bloqueoVigente(idEquipo);
            if (actual != null && !actual.getIdAlumno().equals(alumno)) {
                throw new EquipoEnProcesoException(idEquipo, actual.getIdAlumno(), actual.segundosRestantes());
            }
            ReservaRequest limpio = new ReservaRequest(alumno, idEquipo, r.getHoraInicio(), r.getHoraFin());
            TicketReserva ticket = transaccion.reservar(limpio, host); // commit o rollback dentro
            Consola.info("[RESERVA OK] " + ticket.getNombreEquipo() + " -> " + alumno + " (" + host + ") ticket "
                    + ticket.getCodigoConfirmacion() + " en " + (System.nanoTime() - inicio) / 1_000_000 + " ms");
            return ticket;
            // -------------------------------------------------------------------------------
        } catch (ReservaException e) {
            registrarRechazo(alumno, idEquipo, "RESERVA", e, host);
            throw e;
        } finally {
            // Reserve o no, el EN_PROCESO de ESTE alumno se quita: el equipo queda
            // RESERVADO (si ganó) o vuelve a DISPONIBLE (si falló) para los demás.
            bloqueos.computeIfPresent(idEquipo, (k, b) -> b.getIdAlumno().equals(alumno) ? null : b);
            candado.unlock();
        }
    }

    @Override
    public void cancelarIntento(int idEquipo, String idAlumno) {
        if (idEquipo < 1 || idEquipo > TOTAL_EQUIPOS || idAlumno == null) {
            return;
        }
        String alumno = idAlumno.trim();
        BloqueoTemporal b = bloqueos.get(idEquipo);
        // remove(clave, valor) es atómico: solo quita ESE bloqueo si sigue siendo del alumno
        if (b != null && b.getIdAlumno().equals(alumno) && bloqueos.remove(idEquipo, b)) {
            String host = hostCliente();
            Consola.info("[CANCELA] " + alumno + " soltó PC-" + dos(idEquipo) + " sin reservar -> DISPONIBLE");
            bitacoraDAO.registrarAsync(alumno, idEquipo, "CANCELA_INTENTO", "INFO",
                    "El alumno cerró el diálogo sin confirmar", host);
        }
    }

    @Override
    public void cancelarReserva(int idEquipo, String idAlumno) throws ReservaException {
        String alumno = validarAlumno(idAlumno);
        validarEquipo(idEquipo);
        String host = hostCliente();
        ReentrantLock candado = tomarCandado(idEquipo, alumno, host, "CANCELA_RESERVA");
        try {
            transaccion.cancelar(idEquipo, alumno, host);
            Consola.info("[CANCELA RESERVA] PC-" + dos(idEquipo) + " liberado por su dueño " + alumno);
        } catch (ReservaException e) {
            registrarRechazo(alumno, idEquipo, "CANCELA_RESERVA", e, host);
            throw e;
        } finally {
            candado.unlock();
        }
    }

    @Override
    public TicketReserva consultarTicket(int idEquipo, String idAlumno) throws ReservaException {
        String alumno = validarAlumno(idAlumno);
        validarEquipo(idEquipo);
        try {
            TicketReserva t = reservaDAO.buscarActiva(idEquipo);
            if (t == null || !t.getIdAlumno().equals(alumno)) {
                throw new ReservaException("No hay una reserva activa de PC-" + dos(idEquipo) + " a nombre de " + alumno + ".");
            }
            return t;
        } catch (SQLException e) {
            throw ConexionBD.traducir(e, "consultar el ticket");
        }
    }

    @Override
    public String ping() {
        return "Servidor OK - " + LocalDateTime.now().withNano(0) + " - equipos en proceso: " + bloqueos.size();
    }

    // =====================================================================
    //  APOYO
    // =====================================================================

    /** tryLock con tiempo límite: si otro hilo tarda demasiado, cancelamos esta petición. */
    private ReentrantLock tomarCandado(int idEquipo, String alumno, String host, String accion)
            throws ServicioNoDisponibleException {
        ReentrantLock candado = candados[idEquipo];
        boolean obtenido;
        try {
            obtenido = candado.tryLock(cfg.getLockTimeoutSeg(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            obtenido = false;
        }
        if (!obtenido) {
            ServicioNoDisponibleException ex = new ServicioNoDisponibleException(
                    "TIMEOUT: el equipo PC-" + dos(idEquipo) + " lleva más de " + cfg.getLockTimeoutSeg()
                    + " s ocupado por otra operación (posible BD lenta). Tu petición se canceló para no "
                    + "quedarse bloqueada. Intenta de nuevo.");
            registrarRechazo(alumno, idEquipo, accion, ex, host);
            throw ex;
        }
        return candado;
    }

    /** Devuelve el bloqueo temporal vigente o null; si ya venció, lo elimina. */
    private BloqueoTemporal bloqueoVigente(int idEquipo) {
        BloqueoTemporal b = bloqueos.get(idEquipo);
        if (b != null && b.vencido()) {
            if (bloqueos.remove(idEquipo, b)) {
                anunciarExpiracion(idEquipo, b);
            }
            return null;
        }
        return b;
    }

    /** Lo ejecuta el hilo "limpiador" cada 2 s: libera los EN_PROCESO vencidos. */
    private void liberarBloqueosVencidos() {
        try {
            for (Map.Entry<Integer, BloqueoTemporal> e : bloqueos.entrySet()) {
                BloqueoTemporal b = e.getValue();
                if (b.vencido() && bloqueos.remove(e.getKey(), b)) {
                    anunciarExpiracion(e.getKey(), b);
                }
            }
        } catch (RuntimeException ex) {
            Consola.error("Limpiador de bloqueos: " + ex);
        }
    }

    private void anunciarExpiracion(int idEquipo, BloqueoTemporal b) {
        Consola.info("[EXPIRA] PC-" + dos(idEquipo) + ": " + b.getIdAlumno() + " (" + b.getHost()
                + ") no confirmó en " + cfg.getSegundosBloqueoTemporal() + " s -> vuelve a DISPONIBLE");
        bitacoraDAO.registrarAsync(b.getIdAlumno(), idEquipo, "BLOQUEO_EXPIRADO", "INFO",
                "No confirmó a tiempo (¿cliente desconectado?); equipo liberado", b.getHost());
    }

    private void aplicarBloqueoTemporal(EquipoDTO e) {
        BloqueoTemporal b = bloqueos.get(e.getIdEquipo());
        if (b != null && !b.vencido() && e.getEstado() == EstadoEquipo.DISPONIBLE) {
            e.setEstado(EstadoEquipo.EN_PROCESO);
            e.setOcupadoPor(b.getIdAlumno());
            e.setSegundosRestantesBloqueo(b.segundosRestantes());
        }
    }

    private EquipoDTO buscarEquipo(int idEquipo) throws ReservaException {
        try {
            EquipoDTO e = equipoDAO.buscar(idEquipo);
            if (e == null) {
                throw new ReservaException("El equipo " + idEquipo + " no existe en la base de datos.");
            }
            return e;
        } catch (SQLException ex) {
            throw ConexionBD.traducir(ex, "consultar el equipo");
        }
    }

    private void registrarRechazo(String alumno, int idEquipo, String accion, ReservaException e, String host) {
        String tipo = e.getClass().getSimpleName();
        String resultado = (e instanceof ServicioNoDisponibleException) ? "ERROR" : "RECHAZADO";
        Consola.info("[" + resultado + "] " + accion + " PC-" + dos(idEquipo) + " de " + alumno + " (" + host + "): " + tipo);
        bitacoraDAO.registrarAsync(alumno, idEquipo, accion, resultado, tipo + ": " + e.getMessage(), host);
    }

    private static String validarAlumno(String idAlumno) throws ReservaException {
        if (idAlumno == null || idAlumno.isBlank()) {
            throw new ReservaException("Escribe tu ID de alumno.");
        }
        String limpio = idAlumno.trim();
        if (!limpio.matches("[A-Za-z0-9_-]{1,20}")) {
            throw new ReservaException("ID de alumno inválido: usa solo letras, números, '-' o '_' (máx. 20).");
        }
        return limpio;
    }

    private static void validarEquipo(int idEquipo) throws ReservaException {
        if (idEquipo < 1 || idEquipo > TOTAL_EQUIPOS) {
            throw new ReservaException("El equipo " + idEquipo + " no existe (válidos: 1 a " + TOTAL_EQUIPOS + ").");
        }
    }

    private static void validarHorario(ReservaRequest r) throws ReservaException {
        if (r.getHoraInicio() == null || r.getHoraFin() == null) {
            throw new ReservaException("Debes indicar hora de inicio y hora de fin.");
        }
        if (!r.getHoraFin().isAfter(r.getHoraInicio())) {
            throw new ReservaException("La hora de fin (" + r.getHoraFin().format(HORA)
                    + ") debe ser posterior a la de inicio (" + r.getHoraInicio().format(HORA) + ").");
        }
        if (Duration.between(r.getHoraInicio(), r.getHoraFin()).toHours() > 8) {
            throw new ReservaException("Una reserva no puede durar más de 8 horas.");
        }
    }

    /** IP del cliente que hizo la llamada (RMI la conoce porque atiende su conexión). */
    private static String hostCliente() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            return "local";
        }
    }

    private static String dos(int n) {
        return String.format("%02d", n);
    }

    /** Resumen para el comando "estado" de la consola del servidor. */
    public String estadoInterno() {
        StringBuilder sb = new StringBuilder();
        int ocupados = 0;
        for (int i = 1; i <= TOTAL_EQUIPOS; i++) {
            if (candados[i].isLocked()) {
                ocupados++;
                sb.append("   candado PC-").append(dos(i)).append(" TOMADO, hilos esperando: ")
                  .append(candados[i].getQueueLength()).append('\n');
            }
        }
        sb.insert(0, "  Candados (ReentrantLock) tomados ahora: " + ocupados + "\n");
        sb.append("  Equipos EN_PROCESO: ").append(bloqueos.size()).append('\n');
        bloqueos.forEach((id, b) -> sb.append("   PC-").append(dos(id)).append(" por ").append(b.getIdAlumno())
                .append(" (").append(b.getHost()).append("), quedan ").append(b.segundosRestantes()).append(" s\n"));
        return sb.toString();
    }

    public void cerrar() {
        limpiador.shutdownNow();
        bitacoraDAO.cerrar();
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignorada) {
            // ya no estaba exportado
        }
    }
}
