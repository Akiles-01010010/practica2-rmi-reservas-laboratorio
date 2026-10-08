package servidor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Genera el código de confirmación y la FIRMA DIGITAL SIMULADA del ticket.
 *
 * firma = SHA-256( datosCanónicosDelTicket + "|" + secretoDelServidor )
 *
 * No es una firma digital real (eso usaría un par de llaves RSA/ECDSA), pero
 * sirve para demostrar integridad: si alguien altera un dato del ticket,
 * el hash recalculado por el servidor ya no coincide.
 */
public final class FirmaDigital {

    private static final SecureRandom AZAR = new SecureRandom();
    private static final DateTimeFormatter FORMATO_CODIGO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FirmaDigital() {
    }

    /** Ejemplo: RSV-20261007-153012-PC07-A1B2 */
    public static String generarCodigo(int idEquipo, LocalDateTime marca) {
        return String.format("RSV-%s-PC%02d-%04X", marca.format(FORMATO_CODIGO), idEquipo, AZAR.nextInt(0x10000));
    }

    public static String firmar(String datosCanonicos, String secreto) {
        return sha256(datosCanonicos + "|" + secreto);
    }

    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // 64 caracteres hexadecimales
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no soporta SHA-256", e);
        }
    }
}
