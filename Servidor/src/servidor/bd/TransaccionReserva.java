package servidor.bd;

import comun.EquipoDTO;
import comun.EquipoEnMantenimientoException;
import comun.EstadoEquipo;
import comun.ReservaDuplicadaException;
import comun.ReservaException;
import comun.ReservaRequest;
import comun.ServicioNoDisponibleException;
import comun.TicketReserva;
import servidor.Configuracion;
import servidor.Consola;
import servidor.FirmaDigital;
import servidor.dao.BitacoraDAO;
import servidor.dao.EquipoDAO;
import servidor.dao.ReservaDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * PERSISTENCIA TRANSACCIONAL.
 *
 * Analogía: una transferencia bancaria. O se hacen las DOS cosas (cambiar el
 * estado del equipo + guardar el ticket + anotar en bitácora) o no se hace
 * NINGUNA. Para eso:
 *   setAutoCommit(false)  -> "abro la transacción"
 *   commit()              -> "todo salió bien, guárdalo de forma permanente"
 *   rollback()            -> "algo falló, deshaz todo como si nada hubiera pasado"
 */
public class TransaccionReserva {

    private final ConexionBD bd;
    private final EquipoDAO equipoDAO;
    private final ReservaDAO reservaDAO;
    private final BitacoraDAO bitacoraDAO;
    private final Configuracion cfg;

    public TransaccionReserva(ConexionBD bd, EquipoDAO equipoDAO, ReservaDAO reservaDAO,
                              BitacoraDAO bitacoraDAO, Configuracion cfg) {
        this.bd = bd;
        this.equipoDAO = equipoDAO;
        this.reservaDAO = reservaDAO;
        this.bitacoraDAO = bitacoraDAO;
        this.cfg = cfg;
    }

    /** Reserva atómica: equipo DISPONIBLE -> RESERVADO + INSERT del ticket + bitácora. */
    public TicketReserva reservar(ReservaRequest r, String clienteHost) throws ReservaException {
        Connection con = null;
        try {
            con = bd.abrir();
            con.setAutoCommit(false);                                      // 1) BEGIN

            EquipoDTO equipo = equipoDAO.bloquearFila(con, r.getIdEquipo()); // 2) SELECT ... FOR UPDATE
            if (equipo == null) {
                throw new ReservaException("El equipo " + r.getIdEquipo() + " no existe.");
            }
            if (equipo.getEstado() == EstadoEquipo.MANTENIMIENTO) {
                throw new EquipoEnMantenimientoException(r.getIdEquipo());
            }
            if (equipo.getEstado() == EstadoEquipo.RESERVADO) {
                throw new ReservaDuplicadaException(r.getIdEquipo(), reservaDAO.alumnoConReservaActiva(con, r.getIdEquipo()));
            }

            // 3) Construir el ticket con código, marca de tiempo y firma SHA-256
            LocalDateTime marca = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            String codigo = FirmaDigital.generarCodigo(r.getIdEquipo(), marca);
            String firma = FirmaDigital.firmar(TicketReserva.datosCanonicos(codigo, r.getIdAlumno(),
                    r.getIdEquipo(), r.getHoraInicio(), r.getHoraFin(), marca), cfg.getSecretoFirma());
            TicketReserva ticket = new TicketReserva(codigo, r.getIdAlumno(), r.getIdEquipo(), equipo.getNombre(),
                    r.getHoraInicio(), r.getHoraFin(), marca, firma);

            // 4) UPDATE condicionado: solo si sigue DISPONIBLE (doble seguro)
            if (equipoDAO.cambiarEstado(con, r.getIdEquipo(), EstadoEquipo.DISPONIBLE, EstadoEquipo.RESERVADO) != 1) {
                throw new ReservaDuplicadaException(r.getIdEquipo(), null);
            }
            reservaDAO.insertar(con, ticket, clienteHost);                 // 5) INSERT ticket
            bitacoraDAO.registrar(con, r.getIdAlumno(), r.getIdEquipo(), "RESERVA", "EXITO",
                    "Ticket " + codigo + " (" + r.getHoraInicio().toLocalTime() + "-" + r.getHoraFin().toLocalTime() + ")",
                    clienteHost);                                         // 6) INSERT bitácora

            demoraDidactica(r);                                           // (solo para la prueba 2)

            con.commit();                                                 // 7) COMMIT: todo o nada
            return ticket;

        } catch (SQLIntegrityConstraintViolationException e) {
            ConexionBD.rollbackSilencioso(con);
            // El UNIQUE "uk_una_activa_x_equipo" detectó una segunda reserva activa
            throw new ReservaDuplicadaException(r.getIdEquipo(), null);
        } catch (SQLException e) {
            ConexionBD.rollbackSilencioso(con);
            Consola.error("ROLLBACK de la reserva de PC-" + String.format("%02d", r.getIdEquipo())
                    + " por error SQL: " + e.getMessage());
            throw ConexionBD.traducir(e, "guardar la reserva");
        } catch (ReservaException e) {
            ConexionBD.rollbackSilencioso(con);
            throw e;
        } catch (RuntimeException e) {
            ConexionBD.rollbackSilencioso(con);
            throw new ReservaException("Error interno del servidor: " + e);
        } finally {
            ConexionBD.cerrarSilencioso(con);
        }
    }

    /** Cancelación atómica de una reserva confirmada: reserva CANCELADA + equipo DISPONIBLE. */
    public void cancelar(int idEquipo, String idAlumno, String clienteHost) throws ReservaException {
        Connection con = null;
        try {
            con = bd.abrir();
            con.setAutoCommit(false);
            EquipoDTO equipo = equipoDAO.bloquearFila(con, idEquipo);
            if (equipo == null || equipo.getEstado() != EstadoEquipo.RESERVADO) {
                throw new ReservaException("El equipo PC-" + String.format("%02d", idEquipo) + " no está reservado.");
            }
            if (reservaDAO.cancelarActiva(con, idEquipo, idAlumno) != 1) {
                throw new ReservaException("La reserva de PC-" + String.format("%02d", idEquipo)
                        + " no está a nombre de " + idAlumno + "; solo su dueño puede cancelarla.");
            }
            equipoDAO.cambiarEstado(con, idEquipo, EstadoEquipo.RESERVADO, EstadoEquipo.DISPONIBLE);
            bitacoraDAO.registrar(con, idAlumno, idEquipo, "CANCELA_RESERVA", "EXITO", "Reserva cancelada por su dueño", clienteHost);
            con.commit();
        } catch (SQLException e) {
            ConexionBD.rollbackSilencioso(con);
            throw ConexionBD.traducir(e, "cancelar la reserva");
        } catch (ReservaException e) {
            ConexionBD.rollbackSilencioso(con);
            throw e;
        } finally {
            ConexionBD.cerrarSilencioso(con);
        }
    }

    /**
     * Solo para la PRUEBA 2: deja la transacción abierta N ms ANTES del commit
     * (reserva.demoraTransaccionMs en servidor.properties). En uso normal vale 0.
     */
    private void demoraDidactica(ReservaRequest r) throws ServicioNoDisponibleException {
        int ms = cfg.getDemoraTransaccionMs();
        if (ms <= 0) return;
        Consola.aviso("[DEMO] Transacción de " + r.getIdAlumno() + " abierta (fila PC-"
                + String.format("%02d", r.getIdEquipo()) + " bloqueada). Commit en " + ms + " ms... "
                + "¡Desconecta la red del cliente AHORA!");
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException("La transacción fue interrumpida y se revirtió.");
        }
    }
}
