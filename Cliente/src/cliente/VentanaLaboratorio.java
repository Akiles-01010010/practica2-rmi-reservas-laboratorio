package cliente;

import comun.EquipoDTO;
import comun.EstadoEquipo;
import comun.IReservaService;
import comun.ReservaRequest;
import comun.TicketReserva;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * CAPA 1 - PRESENTACIÓN. Plano interactivo del laboratorio (4 filas x 5 columnas).
 *
 * Regla de oro de Swing: NUNCA hacer llamadas de red en el hilo de la interfaz (EDT),
 * porque la ventana se "congela". Por eso cada llamada RMI va en un SwingWorker.
 */
public class VentanaLaboratorio extends JFrame {

    private static final int REFRESCO_MS = 3000;
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConexionServidor conexion;
    private final BotonEquipo[] botones = new BotonEquipo[IReservaService.TOTAL_EQUIPOS + 1];
    private final JTextField txtAlumno = new JTextField(10);
    private final JComboBox<LocalTime> cmbInicio = new JComboBox<>();
    private final JComboBox<LocalTime> cmbFin = new JComboBox<>();
    private final JCheckBox chkAuto = new JCheckBox("Refresco automático (" + REFRESCO_MS / 1000 + " s)", true);
    private final JLabel lblConexion = new JLabel();
    private final JLabel lblEstado = new JLabel(" ");
    private final AtomicBoolean refrescando = new AtomicBoolean(false);
    private boolean ocupado = false;

    public VentanaLaboratorio(ConexionServidor conexion) {
        super("Laboratorio de Cómputo - Reservas con Java RMI");
        this.conexion = conexion;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPlano(), BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);

        new Timer(REFRESCO_MS, e -> {
            if (chkAuto.isSelected()) refrescar(false);
        }).start();
        refrescar(false);
    }

    // ------------------------------------------------------------------ construcción de la GUI

    private JPanel crearPanelSuperior() {
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila1.add(new JLabel("Servidor:"));
        JLabel srv = new JLabel(conexion.getHost() + ":" + conexion.getPuerto());
        srv.setFont(srv.getFont().deriveFont(Font.BOLD));
        fila1.add(srv);
        fila1.add(lblConexion);
        marcarConexion(false, "conectando...");

        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila2.add(new JLabel("ID de alumno:"));
        txtAlumno.setToolTipText("Letras, números, '-' o '_' (máx. 20)");
        fila2.add(txtAlumno);
        for (int h = 7; h <= 21; h++) {
            cmbInicio.addItem(LocalTime.of(h, 0));
            cmbFin.addItem(LocalTime.of(h, 0));
            if (h < 21) {
                cmbInicio.addItem(LocalTime.of(h, 30));
                cmbFin.addItem(LocalTime.of(h, 30));
            }
        }
        int ahora = Math.min(Math.max(LocalTime.now().getHour(), 7), 19);
        cmbInicio.setSelectedItem(LocalTime.of(ahora, 0));
        cmbFin.setSelectedItem(LocalTime.of(ahora + 2, 0));
        fila2.add(new JLabel("Hora inicio:"));
        fila2.add(cmbInicio);
        fila2.add(new JLabel("Hora fin:"));
        fila2.add(cmbFin);
        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> refrescar(true));
        txtAlumno.addActionListener(e -> refrescar(true));
        fila2.add(btnActualizar);
        fila2.add(chkAuto);

        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        panel.add(fila1);
        panel.add(fila2);
        return panel;
    }

    private JPanel crearPlano() {
        JLabel pizarron = new JLabel("PIZARRÓN  /  FRENTE DEL LABORATORIO", SwingConstants.CENTER);
        pizarron.setOpaque(true);
        pizarron.setBackground(new Color(45, 55, 72));
        pizarron.setForeground(Color.WHITE);
        pizarron.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel rejilla = new JPanel(new GridLayout(IReservaService.FILAS, IReservaService.COLUMNAS, 10, 10));
        for (int i = 1; i <= IReservaService.TOTAL_EQUIPOS; i++) {
            BotonEquipo b = new BotonEquipo(i);
            b.addActionListener(e -> alPulsarEquipo(b));
            botones[i] = b;
            rejilla.add(b);
        }
        JPanel plano = new JPanel(new BorderLayout(0, 10));
        plano.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        plano.add(pizarron, BorderLayout.NORTH);
        plano.add(rejilla, BorderLayout.CENTER);
        return plano;
    }

    private JPanel crearPanelInferior() {
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 2));
        leyenda.add(chip(BotonEquipo.VERDE, "Disponible"));
        leyenda.add(chip(BotonEquipo.AMARILLO, "En proceso (alguien reservando)"));
        leyenda.add(chip(BotonEquipo.ROJO, "Reservado"));
        leyenda.add(chip(BotonEquipo.GRIS, "Mantenimiento"));
        leyenda.add(new JLabel("Marco azul = tu reserva"));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(leyenda, BorderLayout.NORTH);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 12, 6, 12));
        sur.add(lblEstado, BorderLayout.SOUTH);
        return sur;
    }

    private static JLabel chip(Color color, String texto) {
        JLabel l = new JLabel("  " + texto + "  ");
        l.setOpaque(true);
        l.setBackground(color);
        l.setForeground(color == BotonEquipo.AMARILLO ? Color.BLACK : Color.WHITE);
        return l;
    }

    // ------------------------------------------------------------------ refresco del plano

    /** Pide el estado al servidor. manual = true muestra JOptionPane si falla. */
    private void refrescar(boolean manual) {
        if (!refrescando.compareAndSet(false, true)) return; // ya hay uno en curso
        new SwingWorker<List<EquipoDTO>, Void>() {
            @Override
            protected List<EquipoDTO> doInBackground() throws Exception {
                return conexion.llamar(IReservaService::obtenerEquipos);
            }

            @Override
            protected void done() {
                refrescando.set(false);
                try {
                    pintar(get());
                } catch (Exception ex) {
                    marcarConexion(false, "sin conexión");
                    lblEstado.setText("⚠ " + MensajesError.resumen(ex));
                    lblEstado.setForeground(BotonEquipo.ROJO);
                    for (int i = 1; i <= IReservaService.TOTAL_EQUIPOS; i++) botones[i].sinConexion();
                    if (manual) MensajesError.mostrar(VentanaLaboratorio.this, ex, servidor());
                }
            }
        }.execute();
    }

    private void pintar(List<EquipoDTO> equipos) {
        String alumno = txtAlumno.getText().trim();
        int libres = 0, proceso = 0, reservados = 0, mant = 0;
        for (EquipoDTO e : equipos) {
            botones[e.getIdEquipo()].actualizar(e, alumno);
            switch (e.getEstado()) {
                case DISPONIBLE: libres++; break;
                case EN_PROCESO: proceso++; break;
                case RESERVADO: reservados++; break;
                default: mant++;
            }
        }
        marcarConexion(true, "conectado");
        lblEstado.setForeground(getForeground());
        lblEstado.setText("Última actualización " + LocalTime.now().format(HMS) + "  —  " + libres + " disponibles, "
                + proceso + " en proceso, " + reservados + " reservados, " + mant + " en mantenimiento");
    }

    private void marcarConexion(boolean ok, String texto) {
        lblConexion.setText("● " + texto);
        lblConexion.setForeground(ok ? BotonEquipo.VERDE : BotonEquipo.ROJO);
    }

    // ------------------------------------------------------------------ flujo de reserva

    private void alPulsarEquipo(BotonEquipo boton) {
        if (ocupado) return;
        String alumno = alumnoValido();
        if (alumno == null) return;
        EquipoDTO eq = boton.getEquipo();
        int id = boton.getIdEquipo();

        // Si es MI reserva: ver ticket o cancelarla
        if (eq != null && eq.getEstado() == EstadoEquipo.RESERVADO && alumno.equals(eq.getOcupadoPor())) {
            opcionesMiReserva(id, eq.getNombre(), alumno);
            return;
        }
        LocalDateTime[] horario = horarioSeleccionado();
        if (horario == null) return;

        // PASO 1: pedir al servidor que ponga el equipo EN_PROCESO a mi nombre.
        // El servidor decide (aunque mi plano esté desactualizado): puede responder con
        // EquipoEnProceso, ReservaDuplicada o EquipoEnMantenimiento.
        ejecutar("Apartando PC-" + String.format("%02d", id) + "...",
                s -> s.iniciarReserva(id, alumno),
                dto -> confirmar(dto, alumno, horario));
    }

    private void confirmar(EquipoDTO dto, String alumno, LocalDateTime[] horario) {
        refrescar(false); // que se vea en amarillo
        DialogoConfirmacion d = new DialogoConfirmacion(this, dto, alumno, horario[0], horario[1],
                dto.getSegundosRestantesBloqueo());
        d.setVisible(true); // modal: espera aquí hasta que se cierre
        int id = dto.getIdEquipo();

        if (d.getResultado() == DialogoConfirmacion.Resultado.CONFIRMAR) {
            // PASO 2: confirmar -> sección crítica + transacción en el servidor
            ReservaRequest req = new ReservaRequest(alumno, id, horario[0], horario[1]);
            ejecutar("Confirmando reserva de " + dto.getNombre() + "...",
                    s -> s.apartarEquipo(req),
                    ticket -> new VentanaTicket(this, ticket).setVisible(true));
        } else {
            // Canceló, cerró la ventana o se acabó el tiempo: el equipo vuelve a DISPONIBLE
            ejecutarSilencioso(s -> { s.cancelarIntento(id, alumno); return null; });
            if (d.getResultado() == DialogoConfirmacion.Resultado.EXPIRADO) {
                JOptionPane.showMessageDialog(this, "Se acabó el tiempo para confirmar.\n"
                        + dto.getNombre() + " se liberó para los demás alumnos.",
                        "Tiempo agotado", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void opcionesMiReserva(int id, String nombre, String alumno) {
        Object[] opciones = {"Ver ticket", "Cancelar mi reserva", "Cerrar"};
        int op = JOptionPane.showOptionDialog(this, nombre + " está reservado a tu nombre.\n¿Qué deseas hacer?",
                "Mi reserva", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (op == 0) {
            ejecutar("Consultando ticket...", s -> s.consultarTicket(id, alumno),
                    t -> new VentanaTicket(this, t).setVisible(true));
        } else if (op == 1) {
            int ok = JOptionPane.showConfirmDialog(this, "¿Seguro que quieres liberar " + nombre + "?",
                    "Cancelar reserva", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                ejecutar("Cancelando reserva...", s -> { s.cancelarReserva(id, alumno); return Boolean.TRUE; },
                        r -> JOptionPane.showMessageDialog(this, nombre + " quedó DISPONIBLE otra vez."));
            }
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Ejecuta una llamada remota en segundo plano; si falla muestra el error con JOptionPane. */
    private <T> void ejecutar(String mensaje, ConexionServidor.LlamadaRemota<T> llamada, Consumer<T> alTerminar) {
        ocupado = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblEstado.setText(mensaje);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return conexion.llamar(llamada);
            }

            @Override
            protected void done() {
                ocupado = false;
                setCursor(Cursor.getDefaultCursor());
                try {
                    T resultado = get();
                    refrescar(false);
                    alTerminar.accept(resultado);
                } catch (Exception ex) {
                    refrescar(false);
                    MensajesError.mostrar(VentanaLaboratorio.this, ex, servidor());
                }
            }
        }.execute();
    }

    /** Igual, pero sin mostrar errores (por ejemplo al cancelar un intento). */
    private void ejecutarSilencioso(ConexionServidor.LlamadaRemota<Object> llamada) {
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                return conexion.llamar(llamada);
            }

            @Override
            protected void done() {
                refrescar(false);
            }
        }.execute();
    }

    private String alumnoValido() {
        String a = txtAlumno.getText().trim();
        if (a.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe tu ID de alumno antes de reservar.",
                    "Falta tu ID", JOptionPane.WARNING_MESSAGE);
            txtAlumno.requestFocus();
            return null;
        }
        if (!a.matches("[A-Za-z0-9_-]{1,20}")) {
            JOptionPane.showMessageDialog(this, "El ID solo puede tener letras, números, '-' o '_' (máx. 20).",
                    "ID inválido", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return a;
    }

    private LocalDateTime[] horarioSeleccionado() {
        LocalTime ini = (LocalTime) cmbInicio.getSelectedItem();
        LocalTime fin = (LocalTime) cmbFin.getSelectedItem();
        if (ini == null || fin == null || !fin.isAfter(ini)) {
            JOptionPane.showMessageDialog(this, "La hora de fin debe ser posterior a la hora de inicio.",
                    "Horario inválido", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        LocalDate hoy = LocalDate.now();
        return new LocalDateTime[]{hoy.atTime(ini), hoy.atTime(fin)};
    }

    private String servidor() {
        return conexion.getHost() + ":" + conexion.getPuerto();
    }
}
