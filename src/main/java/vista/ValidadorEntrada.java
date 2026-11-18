package vista;

import modelo.AccionAbrirCofre;

import java.util.regex.Pattern;

/**
 * Valida con expresiones regulares lo que escribe el usuario antes de enviarlo a la API o al modelo.
 * Todos los metodos devuelven true/false y no imprimen nada.
 */
public class ValidadorEntrada {

    /** Nombre: de 1 a 80 caracteres (se evalua ya sin espacios al inicio ni al final). */
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^.{1,80}$");

    /** Alias: de 3 a 20 caracteres con letras, digitos, guion o guion bajo. */
    private static final Pattern PATRON_ALIAS = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");

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

    /**
     * Indica si la contrasena tiene formato admisible: de 4 a 20 caracteres con el patron
     * letras/digitos, guion opcional, letras/digitos. Si el formato es valido pero la contrasena
     * es incorrecta se cobra una accion. Si el formato es invalido no se cobra.
     */
    public boolean esFormatoContrasenaValido(String contrasena) {
        // la regla esta en el modelo, aqui solo se reutiliza
        return AccionAbrirCofre.formatoValido(AccionAbrirCofre.normalizar(contrasena));
    }
}