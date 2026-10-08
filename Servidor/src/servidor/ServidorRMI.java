package servidor;

import comun.EquipoDTO;
import comun.IReservaService;
import servidor.bd.ConexionBD;
import servidor.dao.EquipoDAO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * CLASE PRINCIPAL DEL SERVIDOR.
 *  1. Lee servidor.properties.
 *  2. Fija java.rmi.server.hostname (la IP que irá dentro del stub).
 *  3. Comprueba MySQL y muestra el estado LEÍDO DE LA BD (prueba 3: reinicio).
 *  4. Crea el registro RMI (puerto 1099) y publica el objeto remoto (puerto 1100).
 *  5. Queda escuchando; en la consola acepta: estado | equipos | salir
 *
 * Uso: java -Djava.rmi.server.hostname=IP -cp ... servidor.ServidorRMI [servidor.properties]
 */
public class ServidorRMI {

    public static void main(String[] args) {
        Configuracion cfg = Configuracion.cargar(args.length > 0 ? args[0] : "servidor.properties");

        // --- 1) IP pública del servidor para los stubs ---
        String host = System.getProperty("java.rmi.server.hostname");
        if (host == null || host.isBlank()) {
            host = cfg.getRmiHostname();
            if (!host.isBlank()) {
                System.setProperty("java.rmi.server.hostname", host); // ANTES de exportar cualquier objeto
            } else {
                Consola.aviso("No se definió java.rmi.server.hostname: si tienes varias tarjetas de red, "
                        + "los clientes remotos podrían recibir una IP equivocada.");
                host = "(automática)";
            }
        }

        Consola.linea("==============================================================");
        Consola.linea("  SERVIDOR RMI - Reservas e Inventario de Laboratorio");
        Consola.linea("  IP anunciada a clientes : " + host);
        Consola.linea("  Registro RMI            : puerto " + cfg.getPuertoRegistro());
        Consola.linea("  Objeto remoto           : puerto " + cfg.getPuertoObjeto());
        Consola.linea("  Base de datos           : " + cfg.getDbUrl().replaceAll("\\?.*", ""));
        Consola.linea("==============================================================");

        // --- 2) Capa 3: comprobar driver y BD, y leer el estado persistido ---
        try {
            ConexionBD.verificarDriver();
        } catch (ClassNotFoundException e) {
            Consola.error("No se encontró el driver de MySQL (com.mysql.cj.jdbc.Driver). "
                    + "Revisa que lib\\mysql-connector-j.jar esté en el -cp.");
            System.exit(1);
        }
        ConexionBD bd = new ConexionBD(cfg);
        try {
            List<EquipoDTO> equipos = new EquipoDAO(bd).listar();
            Consola.info("Conexión a MySQL OK. Estado del inventario LEÍDO DESDE LA BASE DE DATOS:");
            imprimirPlano(equipos);
        } catch (SQLException e) {
            Consola.error("No se pudo conectar a MySQL: " + e.getMessage());
            Consola.error("¿Está encendido el contenedor? Ejecuta 00_mysql_crear_o_iniciar.bat");
            System.exit(1);
        }

        // --- 3) Capa 2: exportar el objeto remoto y publicarlo en el registro ---
        ReservaServiceImpl servicio;
        try {
            servicio = new ReservaServiceImpl(cfg, bd);
            Registry registro = LocateRegistry.createRegistry(cfg.getPuertoRegistro());
            registro.rebind(IReservaService.NOMBRE_SERVICIO, servicio);
        } catch (ExportException e) {
            Consola.error("El puerto " + cfg.getPuertoRegistro() + " o " + cfg.getPuertoObjeto()
                    + " ya está en uso (¿otro servidor abierto?). Detalle: " + e.getMessage());
            System.exit(1);
            return;
        } catch (RemoteException e) {
            Consola.error("No se pudo iniciar RMI: " + e);
            System.exit(1);
            return;
        }

        Consola.info("Servicio '" + IReservaService.NOMBRE_SERVICIO + "' publicado. Esperando clientes...");
        Consola.linea("Comandos: estado | equipos | salir");

        final ReservaServiceImpl servicioFinal = servicio;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Consola.info("Apagando servidor (los bloqueos EN_PROCESO en memoria se descartan; "
                    + "la BD conserva reservas confirmadas).");
            servicioFinal.cerrar();
        }, "apagado"));

        // --- 4) Consola de administración ---
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String linea;
            while ((linea = in.readLine()) != null) {
                switch (linea.trim().toLowerCase()) {
                    case "estado":
                        Consola.linea(servicio.estadoInterno());
                        break;
                    case "equipos":
                        try {
                            imprimirPlano(servicio.obtenerEquipos());
                        } catch (Exception e) {
                            Consola.error(e.getMessage());
                        }
                        break;
                    case "salir":
                        System.exit(0);
                        break;
                    case "":
                        break;
                    default:
                        Consola.linea("Comandos: estado | equipos | salir");
                }
            }
            // Sin consola (stdin cerrado): el servidor sigue vivo
            new CountDownLatch(1).await();
        } catch (Exception e) {
            Consola.error("Consola: " + e.getMessage());
        }
    }

    /** Dibuja el plano 4x5 en texto. */
    private static void imprimirPlano(List<EquipoDTO> equipos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < equipos.size(); i++) {
            EquipoDTO e = equipos.get(i);
            String estado;
            switch (e.getEstado()) {
                case DISPONIBLE: estado = "libre"; break;
                case EN_PROCESO: estado = "PROC:" + e.getOcupadoPor(); break;
                case RESERVADO: estado = "RESV:" + e.getOcupadoPor(); break;
                default: estado = "MANTENIM.";
            }
            sb.append(String.format(" %s %-16s", e.getNombre(), estado.length() > 16 ? estado.substring(0, 16) : estado));
            if ((i + 1) % IReservaService.COLUMNAS == 0) {
                sb.append('\n');
            }
        }
        Consola.linea(sb.toString());
    }
}
