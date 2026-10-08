package servidor;

/**
 * Representa el estado EN_PROCESO de un equipo: "este alumno tiene el equipo
 * apartado mientras llena el formulario". Es como poner tu mochila en una silla
 * de la biblioteca: los demás ven que está ocupada, pero si no vuelves en X
 * minutos, el bibliotecario la quita y la silla queda libre otra vez.
 *
 * Es inmutable (todos sus campos son final), así se puede compartir entre hilos.
 */
public final class BloqueoTemporal {

    private final String idAlumno;
    private final String host;
    private final long creadoEnMillis;
    private final long expiraEnMillis;

    public BloqueoTemporal(String idAlumno, String host, int segundos) {
        this.idAlumno = idAlumno;
        this.host = host;
        this.creadoEnMillis = System.currentTimeMillis();
        this.expiraEnMillis = creadoEnMillis + segundos * 1000L;
    }

    public String getIdAlumno() { return idAlumno; }
    public String getHost() { return host; }

    public boolean vencido() {
        return System.currentTimeMillis() >= expiraEnMillis;
    }

    public int segundosRestantes() {
        long ms = expiraEnMillis - System.currentTimeMillis();
        return (int) Math.max(0, (ms + 999) / 1000);
    }
}
