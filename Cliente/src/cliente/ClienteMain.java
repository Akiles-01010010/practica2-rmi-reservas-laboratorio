package cliente;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada del CLIENTE.
 * Uso: java -cp ... cliente.ClienteMain <IP_DEL_SERVIDOR> [puerto]
 */
public class ClienteMain {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int puerto = 1099;
        if (args.length > 1) {
            try {
                puerto = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido '" + args[1] + "', se usa 1099");
            }
        }
        ConexionServidor.configurarTimeouts();
        aplicarLookAndFeel();
        System.out.println("Cliente RMI -> servidor " + host + ":" + puerto);

        ConexionServidor conexion = new ConexionServidor(host, puerto);
        SwingUtilities.invokeLater(() -> new VentanaLaboratorio(conexion).setVisible(true));
    }

    /** FlatLaf si está en el classpath (lib\flatlaf.jar); si no, Nimbus (viene con Java). */
    private static void aplicarLookAndFeel() {
        try {
            Class.forName("com.formdev.flatlaf.FlatLightLaf").getMethod("setup").invoke(null);
            return;
        } catch (ReflectiveOperationException | LinkageError e) {
            // FlatLaf no disponible: probamos Nimbus
        }
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // Se queda el Look&Feel por defecto
        }
    }
}
