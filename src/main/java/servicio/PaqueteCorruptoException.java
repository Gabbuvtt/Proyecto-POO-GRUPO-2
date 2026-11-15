package servicio;

/** Se lanza cuando el paquete del cofre tiene datos rotos, ausentes o de tamano incorrecto. */
public class PaqueteCorruptoException extends Exception {

    /** Crea la excepcion con su mensaje. */
    public PaqueteCorruptoException(String mensaje) {
        super(mensaje);
    }
}
