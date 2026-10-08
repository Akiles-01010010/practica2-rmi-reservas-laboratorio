package cliente;

import comun.EquipoDTO;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo que aparece cuando el servidor dejó el equipo EN_PROCESO a tu nombre.
 * Muestra una cuenta regresiva: si no confirmas a tiempo, el equipo se libera solo.
 */
public class DialogoConfirmacion extends JDialog {

    public enum Resultado { CONFIRMAR, CANCELAR, EXPIRADO }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Resultado resultado = Resultado.CANCELAR;
    private int restantes;
    private final Timer cuentaRegresiva;

    public DialogoConfirmacion(Frame padre, EquipoDTO equipo, String alumno,
                               LocalDateTime inicio, LocalDateTime fin, int segundosServidor) {
        super(padre, "Confirmar reserva - " + equipo.getNombre(), true);
        // 2 s de margen para que el cliente nunca confirme después de que el servidor ya lo liberó
        this.restantes = Math.max(5, segundosServidor - 2);
        final int total = restantes;

        JLabel cabecera = new JLabel("  " + equipo.getNombre() + " está APARTADO temporalmente para ti");
        cabecera.setOpaque(true);
        cabecera.setBackground(BotonEquipo.AMARILLO);
        cabecera.setForeground(Color.BLACK);
        cabecera.setFont(cabecera.getFont().deriveFont(Font.BOLD, 15f));
        cabecera.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel datos = new JPanel(new GridLayout(0, 2, 8, 6));
        datos.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        datos.add(new JLabel("Alumno:"));         datos.add(negrita(alumno));
        datos.add(new JLabel("Equipo:"));         datos.add(negrita(equipo.getNombre()));
        datos.add(new JLabel("Características:")); datos.add(new JLabel(equipo.getEspecificaciones()));
        datos.add(new JLabel("Horario:"));        datos.add(negrita(inicio.format(HORA) + " a " + fin.format(HORA)
                + " (" + inicio.toLocalDate() + ")"));

        JLabel aviso = new JLabel("<html>Mientras este diálogo esté abierto, los demás ven el equipo en "
                + "<b>AMARILLO</b> y no pueden reservarlo.</html>");
        aviso.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 16));

        JProgressBar barra = new JProgressBar(0, total);
        barra.setValue(total);
        barra.setStringPainted(true);
        barra.setString("Tiempo para confirmar: " + restantes + " s");

        JButton confirmar = new JButton("Confirmar reserva");
        JButton cancelar = new JButton("Cancelar");
        confirmar.addActionListener(e -> { resultado = Resultado.CONFIRMAR; dispose(); });
        cancelar.addActionListener(e -> { resultado = Resultado.CANCELAR; dispose(); });

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(datos, BorderLayout.NORTH);
        centro.add(aviso, BorderLayout.CENTER);
        JPanel pieBarra = new JPanel(new BorderLayout());
        pieBarra.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        pieBarra.add(barra);
        centro.add(pieBarra, BorderLayout.SOUTH);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(cancelar);
        botones.add(confirmar);

        setLayout(new BorderLayout());
        add(cabecera, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(confirmar);

        cuentaRegresiva = new Timer(1000, e -> {
            restantes--;
            barra.setValue(Math.max(0, restantes));
            barra.setString("Tiempo para confirmar: " + Math.max(0, restantes) + " s");
            if (restantes <= 0) {
                resultado = Resultado.EXPIRADO;
                dispose();
            }
        });

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // la "X" cuenta como Cancelar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) { cuentaRegresiva.start(); }

            @Override
            public void windowClosed(WindowEvent e) { cuentaRegresiva.stop(); }
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(padre);
    }

    private static JLabel negrita(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    public Resultado getResultado() {
        return resultado;
    }
}
