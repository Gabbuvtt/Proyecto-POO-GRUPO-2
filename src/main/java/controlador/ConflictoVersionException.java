package controlador;

/** La API rechazo el guardado porque la partida ya tiene una revision mas nueva. */
public class ConflictoVersionException extends RuntimeException {

    /** Crea la excepcion con el mensaje dado. */
    public ConflictoVersionException(String mensaje) {
        super(mensaje);
    }
}
