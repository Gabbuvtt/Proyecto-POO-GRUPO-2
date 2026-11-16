package vista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorEntradaTest {

    private final ValidadorEntrada validador = new ValidadorEntrada();

    @Test
    void nombreAceptaDe1a80Caracteres() {
        assertTrue(validador.esNombreValido("Ana Pérez"));
        assertTrue(validador.esNombreValido("a".repeat(80)));
        assertFalse(validador.esNombreValido("a".repeat(81)));
        assertFalse(validador.esNombreValido("   "));
        assertFalse(validador.esNombreValido(null));
    }

    @Test
    void aliasAceptaLetrasDigitosGuionYGuionBajo() {
        assertTrue(validador.esAliasValido("ana_ucab"));
        assertTrue(validador.esAliasValido("ana-ucab"));
        assertFalse(validador.esAliasValido("ab"));
        assertFalse(validador.esAliasValido("a".repeat(21)));
        assertFalse(validador.esAliasValido("ana ucab"));
        assertFalse(validador.esAliasValido("ana@ucab"));
    }

    @Test
    void contrasenaConFormatoAdmisibleAunqueSeaIncorrecta() {
        assertTrue(validador.esFormatoContrasenaValido("ORBITA-7392"));
        assertTrue(validador.esFormatoContrasenaValido("ORBITA7392"));
        assertTrue(validador.esFormatoContrasenaValido("orbita-7392"));
    }

    @Test
    void contrasenaConFormatoInvalidoNoSeAcepta() {
        assertFalse(validador.esFormatoContrasenaValido("AB1"));
        assertFalse(validador.esFormatoContrasenaValido("ORBITA 7392"));
        assertFalse(validador.esFormatoContrasenaValido("ORBITA--7392"));
        assertFalse(validador.esFormatoContrasenaValido("ORBITA#7392"));
        assertFalse(validador.esFormatoContrasenaValido("A".repeat(21)));
    }
}