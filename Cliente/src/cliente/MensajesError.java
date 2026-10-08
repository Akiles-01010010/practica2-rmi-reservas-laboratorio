package cliente;

import comun.EquipoEnMantenimientoException;
import comun.EquipoEnProcesoException;
import comun.ReservaDuplicadaException;
import comun.ReservaException;
import comun.ServicioNoDisponibleException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.concurrent.ExecutionException;

/** Traduce cada excepción a un mensaje CLARO para el alumno (JOptionPane). */
public final class MensajesError {

    private MensajesError() {
    }

    public static Throwable desenvolver(Throwable t) {
        while ((t instanceof ExecutionException || t instanceof InvocationTargetException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    public static void mostrar(Component padre, Throwable error, String servidor) {
        Throwable t = desenvolver(error);
        String titulo;
        String mensaje;
        int tipo;

        if (t instanceof EquipoEnProcesoException) {
            titulo = "Equipo en proceso de reserva";
            mensaje = t.getMessage();
            tipo = JOptionPane.WARNING_MESSAGE;
        } else if (t instanceof ReservaDuplicadaException) {
            titulo = "Reserva duplicada";
            mensaje = t.getMessage();
            tipo = JOptionPane.WARNING_MESSAGE;
        } else if (t instanceof EquipoEnMantenimientoException) {
            titulo = "Equipo en mantenimiento";
            mensaje = t.getMessage();
            tipo = JOptionPane.ERROR_MESSAGE;
        } else if (t instanceof ServicioNoDisponibleException) {
            titulo = "Tiempo de espera agotado (timeout)";
            mensaje = t.getMessage();
            tipo = JOptionPane.ERROR_MESSAGE;
        } else if (t instanceof ReservaException) {
            titulo = "No se pudo completar la operación";
            mensaje = t.getMessage();
            tipo = JOptionPane.WARNING_MESSAGE;
        } else if (t instanceof ConnectException || t instanceof ConnectIOException || t instanceof UnknownHostException
                || t.getCause() instanceof UnknownHostException) {
            titulo = "Sin conexión con el servidor";
            mensaje = "No se pudo conectar con el servidor RMI en " + servidor + ".\n\n"
                    + "Revisa que:\n"
                    + " • El servidor esté encendido (06_ejecutar_servidor.bat).\n"
                    + " • La IP sea correcta y ambas PCs estén conectadas al mismo Wi-Fi / router.\n"
                    + " • El firewall del servidor permita los puertos TCP 1099 y 1100.";
            tipo = JOptionPane.ERROR_MESSAGE;
        } else if (t instanceof NotBoundException) {
            titulo = "Servicio no publicado";
            mensaje = "El registro RMI respondió, pero el servicio no está publicado.\n"
                    + "Probablemente el servidor se está reiniciando. Pulsa Actualizar en unos segundos.";
            tipo = JOptionPane.ERROR_MESSAGE;
        } else if (t instanceof NoSuchObjectException) {
            titulo = "El servidor se reinició";
            mensaje = "El servidor se reinició y la conexión anterior ya no es válida.\n"
                    + "Pulsa Actualizar: el cliente se reconecta solo.";
            tipo = JOptionPane.WARNING_MESSAGE;
        } else if (t instanceof UnmarshalException || t instanceof MarshalException
                || causaEs(t, SocketTimeoutException.class)) {
            titulo = "Se cortó la comunicación";
            mensaje = "Se perdió la conexión mientras el servidor procesaba tu petición\n"
                    + "(o no respondió en " + ConexionServidor.TIMEOUT_RESPUESTA_MS / 1000 + " s).\n\n"
                    + "La reserva PUDO haberse guardado o no. Cuando vuelva la red pulsa Actualizar:\n"
                    + "si el equipo aparece marcado como TUYO, haz clic en él → \"Ver ticket\".";
            tipo = JOptionPane.ERROR_MESSAGE;
        } else if (t instanceof RemoteException) {
            titulo = "Error de comunicación RMI";
            mensaje = "Error de red con el servidor " + servidor + ":\n" + t.getMessage();
            tipo = JOptionPane.ERROR_MESSAGE;
        } else {
            titulo = "Error inesperado";
            mensaje = t.toString();
            tipo = JOptionPane.ERROR_MESSAGE;
        }
        JOptionPane.showMessageDialog(padre, mensaje, titulo, tipo);
    }

    /** Texto corto para la barra de estado. */
    public static String resumen(Throwable error) {
        Throwable t = desenvolver(error);
        if (t instanceof ReservaException) {
            return t.getMessage();
        }
        if (t instanceof ConnectException || t instanceof ConnectIOException) {
            return "Sin conexión con el servidor (reintentando automáticamente...)";
        }
        if (t instanceof NotBoundException || t instanceof NoSuchObjectException) {
            return "El servidor se está reiniciando (reintentando...)";
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private static boolean causaEs(Throwable t, Class<? extends Throwable> tipo) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (tipo.isInstance(c)) return true;
        }
        return false;
    }
}
