package vista;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valida con expresiones regulares lo que escribe el usuario, ANTES de enviarlo a la API o al modelo.
 * Todos los metodos devuelven true/false y no imprimen nada.
 */
public class ValidadorEntrada {

    /** Nombre: de 1 a 80 caracteres (se evalua ya sin espacios al inicio ni al final). */
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^.{1,80}$");

    /** Alias: de 3 a 20 caracteres con letras, digitos, guion o guion bajo. */
    private static final Pattern PATRON_ALIAS = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");

    /** Contrasena del cofre: pista 1, un guion opcional y pista 2, ya en mayusculas. */
    private static final Pattern PATRON_CONTRASENA = Pattern.compile("^[A-Z0-9]+(?:-[A-Z0-9]+)?$");

    private static final int LARGO_MINIMO_CONTRASENA = 4;
    private static final int LARGO_MAXIMO_CONTRASENA = 20;

    /** Indica si el nombre tiene entre 1 y 80 caracteres (ignorando espacios en los extremos). */
    public boolean esNombreValido(String nombre) {
        if (nombre == null) {
            return false;
        }
        return PATRON_NOMBRE.matcher(nombre.trim()).matches();
    }

    /** Indica si el alias tiene de 3 a 20 caracteres con letras, digitos, guion o guion bajo. */
    public boolean esAliasValido(String alias) {
        if (alias == null) {
            return false;
        }
        return PATRON_ALIAS.matcher(alias).matches();
    }

    /** Pasa la contrasena a mayusculas sin quitar espacios (los espacios la dejan invalida). */
    public String normalizarContrasena(String contrasena) {
        if (contrasena == null) {
            return null;
        }
        return contrasena.toUpperCase(Locale.ROOT);
    }

    /**
     * Indica si la contrasena tiene formato admisible: de 4 a 20 caracteres con el patron
     * letras/digitos, guion opcional, letras/digitos. Un formato valido pero incorrecto SI cuesta accion;
     * un formato invalido NO.
     */
    public boolean esFormatoContrasenaValido(String contrasena) {
        String normalizada = normalizarContrasena(contrasena);
        if (normalizada == null) {
            return false;
        }
        int largo = normalizada.length();
        if (largo < LARGO_MINIMO_CONTRASENA || largo > LARGO_MAXIMO_CONTRASENA) {
            return false;
        }
        return PATRON_CONTRASENA.matcher(normalizada).matches();
    }
}