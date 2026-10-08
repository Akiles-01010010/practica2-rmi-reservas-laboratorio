package cliente;

import comun.TicketReserva;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/** Ventana que muestra el TicketReserva (comprobante) recibido del servidor. */
public class VentanaTicket extends JDialog {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public VentanaTicket(Frame padre, TicketReserva t) {
        super(padre, "Comprobante de reserva", true);

        JLabel cabecera = new JLabel("  RESERVA CONFIRMADA");
        cabecera.setOpaque(true);
        cabecera.setBackground(BotonEquipo.VERDE);
        cabecera.setForeground(Color.WHITE);
        cabecera.setFont(cabecera.getFont().deriveFont(Font.BOLD, 18f));
        cabecera.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel datos = new JPanel(new GridBagLayout());
        datos.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 12);
        c.anchor = GridBagConstraints.WEST;
        int fila = 0;
        fila = agregar(datos, c, fila, "Código de confirmación:", t.getCodigoConfirmacion(), true);
        fila = agregar(datos, c, fila, "Alumno:", t.getIdAlumno(), false);
        fila = agregar(datos, c, fila, "Equipo:", t.getNombreEquipo(), false);
        fila = agregar(datos, c, fila, "Horario:", t.getHoraInicio().toLocalDate() + "   "
                + t.getHoraInicio().format(HORA) + " a " + t.getHoraFin().format(HORA), false);
        fila = agregar(datos, c, fila, "Marca de tiempo:", t.getMarcaTiempoTexto(), false);

        c.gridx = 0; c.gridy = fila; c.gridwidth = 2;
        datos.add(new JLabel("Firma digital (SHA-256 simulada):"), c);
        JTextArea firma = new JTextArea(t.getFirmaDigital(), 2, 34);
        firma.setLineWrap(true);
        firma.setEditable(false);
        firma.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        firma.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        c.gridy = fila + 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        datos.add(firma, c);

        JButton copiar = new JButton("Copiar");
        JButton guardar = new JButton("Guardar .txt");
        JButton cerrar = new JButton("Cerrar");
        copiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto(t)), null);
            JOptionPane.showMessageDialog(this, "Ticket copiado al portapapeles.");
        });
        guardar.addActionListener(e -> guardarArchivo(t));
        cerrar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(copiar);
        botones.add(guardar);
        botones.add(cerrar);

        setLayout(new BorderLayout());
        add(cabecera, BorderLayout.NORTH);
        add(datos, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(cerrar);
        pack();
        setLocationRelativeTo(padre);
    }

    private static int agregar(JPanel p, GridBagConstraints c, int fila, String etiqueta, String valor, boolean destacado) {
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy = fila;
        p.add(new JLabel(etiqueta), c);
        JLabel v = new JLabel(valor);
        v.setFont(v.getFont().deriveFont(Font.BOLD, destacado ? 15f : v.getFont().getSize2D()));
        c.gridx = 1;
        p.add(v, c);
        return fila + 1;
    }

    private static String texto(TicketReserva t) {
        return "=== COMPROBANTE DE RESERVA - LABORATORIO ===\n"
                + "Código:          " + t.getCodigoConfirmacion() + "\n"
                + "Alumno:          " + t.getIdAlumno() + "\n"
                + "Equipo:          " + t.getNombreEquipo() + "\n"
                + "Horario:         " + t.getHoraInicio() + " a " + t.getHoraFin() + "\n"
                + "Marca de tiempo: " + t.getMarcaTiempoTexto() + "\n"
                + "Firma SHA-256:   " + t.getFirmaDigital() + "\n";
    }

    private void guardarArchivo(TicketReserva t) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("ticket_" + t.getCodigoConfirmacion() + ".txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(), texto(t).getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(this, "Guardado en " + fc.getSelectedFile());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
