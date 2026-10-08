package servidor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Pequeño "logger" para ver en la consola del servidor qué pasa y a qué hora (con milisegundos). */
public final class Consola {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Consola() {
    }

    public static synchronized void info(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [" + Thread.currentThread().getName() + "] " + mensaje);
    }

    public static synchronized void aviso(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [AVISO] " + mensaje);
    }

    public static synchronized void error(String mensaje) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] [ERROR] " + mensaje);
    }

    public static synchronized void linea(String texto) {
        System.out.println(texto);
    }
}
