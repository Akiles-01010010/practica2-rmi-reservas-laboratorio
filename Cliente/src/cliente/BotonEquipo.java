package cliente;

import comun.EquipoDTO;
import comun.EstadoEquipo;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;

/**
 * Botón que representa UNA computadora del plano. Pinta su propio fondo
 * (así el color se ve igual con cualquier Look&Feel):
 *   Verde = Disponible, Amarillo = En proceso, Rojo = Reservado, Gris = Mantenimiento.
 */
public class BotonEquipo extends JButton {

    public static final Color VERDE = new Color(46, 160, 67);
    public static final Color AMARILLO = new Color(240, 173, 0);
    public static final Color ROJO = new Color(207, 34, 46);
    public static final Color GRIS = new Color(110, 118, 129);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final int idEquipo;
    private EquipoDTO equipo;
    private boolean esMio;

    public BotonEquipo(int idEquipo) {
        this.idEquipo = idEquipo;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.BOTTOM);
        setFont(getFont().deriveFont(Font.PLAIN, 13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(150, 110));
        setText(String.format("<html><center><b>PC-%02d</b><br>sin datos</center></html>", idEquipo));
    }

    public int getIdEquipo() { return idEquipo; }
    public EquipoDTO getEquipo() { return equipo; }

    public void actualizar(EquipoDTO e, String alumnoActual) {
        this.equipo = e;
        this.esMio = alumnoActual != null && alumnoActual.equals(e.getOcupadoPor());
        String linea2 = e.getEstado().getEtiqueta();
        String linea3 = "";
        switch (e.getEstado()) {
            case RESERVADO:
                linea3 = (esMio ? "TUYO · " : e.getOcupadoPor() + " · ")
                        + (e.getHoraInicio() != null ? e.getHoraInicio().format(HORA) + "-" + e.getHoraFin().format(HORA) : "");
                break;
            case EN_PROCESO:
                linea3 = (esMio ? "Tú" : e.getOcupadoPor()) + " · " + e.getSegundosRestantesBloqueo() + " s";
                break;
            case MANTENIMIENTO:
                linea3 = "No disponible";
                break;
            default:
                linea3 = "Clic para reservar";
        }
        setText("<html><center><b style='font-size:13px'>" + e.getNombre() + "</b><br>" + linea2
                + "<br><span style='font-size:10px'>" + linea3 + "</span></center></html>");
        setForeground(e.getEstado() == EstadoEquipo.EN_PROCESO ? Color.BLACK : Color.WHITE);
        setToolTipText("<html><b>" + e.getNombre() + "</b><br>" + e.getEspecificaciones() + "<br>Estado: "
                + e.getEstado().getEtiqueta() + (e.getOcupadoPor() != null ? "<br>Alumno: " + e.getOcupadoPor() : "")
                + "</html>");
        repaint();
    }

    public void sinConexion() {
        this.equipo = null;
        setForeground(Color.WHITE);
        setText(String.format("<html><center><b>PC-%02d</b><br>sin conexión</center></html>", idEquipo));
        repaint();
    }

    private Color colorFondo() {
        if (equipo == null) return new Color(160, 160, 160);
        switch (equipo.getEstado()) {
            case DISPONIBLE: return VERDE;
            case EN_PROCESO: return AMARILLO;
            case RESERVADO: return ROJO;
            default: return GRIS;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fondo = colorFondo();
        if (getModel().isRollover()) fondo = fondo.brighter();
        if (getModel().isPressed()) fondo = fondo.darker();
        int w = getWidth(), h = getHeight();
        g2.setColor(fondo);
        g2.fillRoundRect(2, 2, w - 4, h - 4, 18, 18);

        // Pequeño monitor dibujado (decorativo)
        Color trazo = getForeground();
        g2.setColor(trazo);
        g2.setStroke(new BasicStroke(2f));
        int mw = 34, mh = 22, mx = (w - mw) / 2, my = 10;
        g2.drawRoundRect(mx, my, mw, mh, 4, 4);
        g2.drawLine(w / 2, my + mh, w / 2, my + mh + 5);
        g2.drawLine(w / 2 - 8, my + mh + 5, w / 2 + 8, my + mh + 5);

        if (esMio) { // marco para distinguir MI reserva
            g2.setColor(new Color(30, 90, 220));
            g2.setStroke(new BasicStroke(4f));
            g2.drawRoundRect(3, 3, w - 7, h - 7, 18, 18);
        }
        g2.dispose();
        super.paintComponent(g); // pinta el texto encima
    }
}
