package modelo;

import java.util.Locale;
import java.util.regex.Pattern;

/** Accion de abrir el cofre en X con la contrasena formada por las dos pistas. */
public class AccionAbrirCofre {

    private static final int MIN_LARGO = 4;
    private static final int MAX_LARGO = 20;
    private static final Pattern FORMATO = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)?");

    /** Pasa la contrasena a mayusculas sin quitar espacios. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.toUpperCase(Locale.ROOT);
    }

    /** Indica si la contrasena ya normalizada tiene entre 4 y 20 caracteres y cumple el formato. */
    public static boolean formatoValido(String texto) {
        if (texto == null || texto.length() < MIN_LARGO || texto.length() > MAX_LARGO) {
            return false;
        }
        return FORMATO.matcher(texto).matches();
    }
}
