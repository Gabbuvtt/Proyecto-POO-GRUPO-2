package servicio;

/** Se lanza cuando la contrasena es incorrecta o los datos cifrados fueron alterados. */
public class FalloAutenticacionException extends Exception {

    /** Crea la excepcion con su mensaje. */
    public FalloAutenticacionException(String mensaje) {
        super(mensaje);
    }
}
