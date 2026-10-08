package servidor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Lee servidor.properties y ofrece cada valor con un valor por defecto seguro. */
public class Configuracion {

    private final Properties props = new Properties();

    public static Configuracion cargar(String ruta) {
        Configuracion cfg = new Configuracion();
        Path archivo = Paths.get(ruta);
        if (Files.exists(archivo)) {
            try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                cfg.props.load(lector);
                Consola.info("Configuración leída de " + archivo.toAbsolutePath());
            } catch (IOException e) {
                Consola.error("No se pudo leer " + archivo + ": " + e.getMessage() + " (se usan valores por defecto)");
            }
        } else {
            Consola.aviso("No existe " + archivo.toAbsolutePath() + "; se usan valores por defecto.");
        }
        return cfg;
    }

    private String texto(String clave, String porDefecto) {
        String valor = props.getProperty(clave);
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    private int numero(String clave, int porDefecto) {
        try {
            return Integer.parseInt(texto(clave, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            Consola.aviso("Valor inválido para " + clave + "; se usa " + porDefecto);
            return porDefecto;
        }
    }

    public String getRmiHostname()          { return texto("rmi.hostname", ""); }
    public int getPuertoRegistro()          { return numero("rmi.puertoRegistro", 1099); }
    public int getPuertoObjeto()            { return numero("rmi.puertoObjeto", 1100); }

    public String getDbUrl() {
        return texto("db.url", "jdbc:mysql://127.0.0.1:3310/laboratorio_rmi"
                + "?connectTimeout=3000&socketTimeout=10000&sslMode=DISABLED&allowPublicKeyRetrieval=true");
    }
    public String getDbUsuario()            { return texto("db.usuario", "rmi_user"); }
    public String getDbPassword()           { return texto("db.password", "RmiLab2026"); }
    public int getDbLoginTimeoutSeg()       { return numero("db.loginTimeoutSeg", 5); }
    public int getDbQueryTimeoutSeg()       { return numero("db.queryTimeoutSeg", 5); }
    public int getDbLockWaitTimeoutSeg()    { return numero("db.lockWaitTimeoutSeg", 5); }

    public int getLockTimeoutSeg()          { return numero("reserva.lockTimeoutSeg", 5); }
    public int getSegundosBloqueoTemporal() { return numero("reserva.segundosBloqueoTemporal", 60); }
    public int getDemoraTransaccionMs()     { return numero("reserva.demoraTransaccionMs", 0); }
    public String getSecretoFirma()         { return texto("firma.secreto", "Secreto-Simulado-Lab-RMI-2026"); }
}
