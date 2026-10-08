package cliente;

import comun.IReservaService;
import comun.ReservaException;
import comun.ReservaRequest;
import comun.TicketReserva;

import java.rmi.registry.LocateRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * PRUEBA DE ESTRÉS (concurrencia).
 * Lanza N hilos que llaman a apartarEquipo() sobre el MISMO equipo en el mismo instante
 * (todos esperan detrás de una "línea de salida" = CountDownLatch). Solo uno debe ganar.
 *
 * Uso:
 *   java -cp ... cliente.PruebaConcurrencia <IP_SERVIDOR> <idEquipo> <hilos> <prefijo> [HH:mm:ss]
 *
 *  - prefijo  : identifica a esta computadora en los IDs de alumno (ej. PCA -> PCA-01, PCA-02...).
 *  - HH:mm:ss : (opcional) hora exacta de disparo. Úsala en DOS computadoras con la misma hora
 *               para que ambas disparen a la vez (depende de que sus relojes estén sincronizados).
 */
public class PruebaConcurrencia {

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int idEquipo = args.length > 1 ? Integer.parseInt(args[1]) : 7;
        int hilos = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        String prefijo = args.length > 3 ? args[3] : "HILO";
        String disparo = args.length > 4 ? args[4] : "";

        ConexionServidor.configurarTimeouts();
        IReservaService servicio = (IReservaService) LocateRegistry.getRegistry(host, 1099)
                .lookup(IReservaService.NOMBRE_SERVICIO);
        System.out.println("Conectado: " + servicio.ping());
        System.out.printf("Equipo objetivo: PC-%02d | hilos: %d | prefijo: %s%n", idEquipo, hilos, prefijo);

        LocalDateTime inicio = LocalDate.now().atTime(10, 0);
        LocalDateTime fin = inicio.plusHours(2);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);   // la "pistola de salida"
        CountDownLatch terminados = new CountDownLatch(hilos);
        String[] resultado = new String[hilos];
        String[] horaRespuesta = new String[hilos];
        long[] duracionMs = new long[hilos];
        boolean[] gano = new boolean[hilos];

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        for (int i = 0; i < hilos; i++) {
            final int n = i;
            final String alumno = String.format("%s-%02d", prefijo, i + 1);
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();                         // todos esperan aquí...
                    long t0 = System.nanoTime();
                    try {
                        TicketReserva t = servicio.apartarEquipo(new ReservaRequest(alumno, idEquipo, inicio, fin));
                        gano[n] = true;
                        resultado[n] = "GANÓ  -> " + t.getCodigoConfirmacion();
                    } catch (ReservaException e) {
                        resultado[n] = "perdió -> " + e.getClass().getSimpleName();
                    } catch (Exception e) {
                        resultado[n] = "ERROR  -> " + e;
                    }
                    duracionMs[n] = (System.nanoTime() - t0) / 1_000_000;
                    horaRespuesta[n] = LocalTime.now().format(HMS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    terminados.countDown();
                }
            });
        }
        listos.await();

        if (!disparo.isBlank()) {
            LocalTime objetivo = LocalTime.parse(disparo);
            long espera = Duration.between(LocalTime.now(), objetivo).toMillis();
            System.out.println("Esperando hasta las " + objetivo + " para disparar (" + espera / 1000 + " s)...");
            if (espera > 0) Thread.sleep(espera);
        }
        System.out.println(">>> ¡DISPARO! " + hilos + " hilos llaman a apartarEquipo() a las " + LocalTime.now().format(HMS));
        salida.countDown();                                 // ...y salen TODOS a la vez
        boolean acabo = terminados.await(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        System.out.println();
        System.out.println(" #  | Alumno       | Respuesta     | ms    | Resultado");
        System.out.println("----+--------------+---------------+-------+-------------------------------------------");
        int ganadores = 0;
        for (int i = 0; i < hilos; i++) {
            if (gano[i]) ganadores++;
            System.out.printf("%3d | %-12s | %-13s | %5d | %s%n", i + 1, String.format("%s-%02d", prefijo, i + 1),
                    horaRespuesta[i], duracionMs[i], resultado[i]);
        }
        System.out.println();
        if (!acabo) System.out.println("AVISO: algunos hilos no terminaron en 60 s.");
        System.out.println("Reservas exitosas en ESTA computadora: " + ganadores + " de " + hilos);
        if (ganadores == 1) {
            System.out.println("[OK] Exclusión mutua verificada: solo un hilo obtuvo el equipo.");
        } else if (ganadores == 0) {
            System.out.println("[INFO] Aquí no ganó nadie: el equipo ya estaba reservado/en mantenimiento"
                    + " o lo ganó la OTRA computadora. Revisa la consola del servidor y la bitácora.");
        } else {
            System.out.println("[FALLA] ¡Más de un ganador! Hubo condición de carrera.");
        }
    }
}
