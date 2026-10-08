package comun;

/**
 * Clase base de los errores de NEGOCIO de la práctica.
 * Es una excepción "checked" y Serializable (todas las Exception lo son),
 * así que el servidor la lanza y RMI la entrega tal cual al cliente.
 *
 * IMPORTANTE: nunca le pasamos como "causa" una excepción del driver de MySQL,
 * porque el cliente no tiene ese .jar y fallaría al deserializarla
 * (UnmarshalException / ClassNotFoundException). Solo viaja el mensaje.
 */
public class ReservaException extends Exception {

    private static final long serialVersionUID = 1L;

    public ReservaException(String mensaje) {
        super(mensaje);
    }
}
