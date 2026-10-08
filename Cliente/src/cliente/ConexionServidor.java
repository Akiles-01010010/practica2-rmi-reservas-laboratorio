package cliente;

import comun.IReservaService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;

/**
 * Encapsula la conexión con el servidor:
 *  - Busca el stub en el registro RMI (lookup) y lo guarda.
 *  - Si una llamada falla por red, descarta el stub; la siguiente llamada vuelve a hacer
 *    lookup. Así el cliente se RECONECTA SOLO cuando el servidor se reinicia (prueba 3).
 */
public class ConexionServidor {

    /** Milisegundos máximos para abrir la conexión TCP con el servidor. */
    public static final int TIMEOUT_CONEXION_MS = 5000;
    /** Milisegundos máximos esperando la respuesta de una llamada remota. */
    public static final int TIMEOUT_RESPUESTA_MS = 25000;

    private final String host;
    private final int puerto;
    private IReservaService servicio;

    public ConexionServidor(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    /**
     * Timeouts del lado cliente para que la GUI nunca se quede "congelada" esperando:
     *  - responseTimeout: propiedad de la implementación RMI de OpenJDK (lectura de respuesta).
     *  - Fábrica de sockets con connect() de 5 s (por defecto Windows espera ~21 s).
     * Debe llamarse una sola vez, ANTES de cualquier llamada RMI.
     */
    public static void configurarTimeouts() {
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", String.valueOf(TIMEOUT_RESPUESTA_MS));
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {
                @Override
                public Socket createSocket(String h, int p) throws IOException {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(h, p), TIMEOUT_CONEXION_MS);
                    return s;
                }

                @Override
                public ServerSocket createServerSocket(int p) throws IOException {
                    return new ServerSocket(p);
                }
            });
        } catch (IOException e) {
            // Ya estaba configurada: no pasa nada
        }
    }

    public synchronized IReservaService servicio() throws RemoteException, NotBoundException {
        if (servicio == null) {
            Registry registro = LocateRegistry.getRegistry(host, puerto);
            servicio = (IReservaService) registro.lookup(IReservaService.NOMBRE_SERVICIO);
        }
        return servicio;
    }

    public synchronized void invalidar() {
        servicio = null;
    }

    /** Ejecuta una llamada remota; si falla la RED, invalida el stub para reconectar luego. */
    public <T> T llamar(LlamadaRemota<T> llamada) throws Exception {
        try {
            return llamada.ejecutar(servicio());
        } catch (RemoteException | NotBoundException e) {
            invalidar();
            throw e;
        }
    }

    public String getHost() { return host; }
    public int getPuerto() { return puerto; }

    @FunctionalInterface
    public interface LlamadaRemota<T> {
        T ejecutar(IReservaService s) throws Exception;
    }
}
