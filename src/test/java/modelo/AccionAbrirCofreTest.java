package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccionAbrirCofreTest {

    @Test
    void normalizarPasaAMayusculasSinQuitarEspacios() {
        assertEquals("ORBITA-7392", AccionAbrirCofre.normalizar("orbita-7392"));
        assertEquals(" ORBITA 7392 ", AccionAbrirCofre.normalizar(" orbita 7392 "));
    }

    @Test
    void formatosValidos() {
        assertTrue(AccionAbrirCofre.formatoValido(AccionAbrirCofre.normalizar("orbita-7392")));
        assertTrue(AccionAbrirCofre.formatoValido("ORBITA7392"));
        assertTrue(AccionAbrirCofre.formatoValido("ABCD"));
        assertTrue(AccionAbrirCofre.formatoValido("ABCDEFGHIJ-123456789"));
    }

    @Test
    void formatosInvalidos() {
        assertFalse(AccionAbrirCofre.formatoValido("ABC"));
        assertFalse(AccionAbrirCofre.formatoValido("ORBITA 7392"));
        assertFalse(AccionAbrirCofre.formatoValido("ORB!TA"));
        assertFalse(AccionAbrirCofre.formatoValido("ABCDEFGHIJ-1234567890"));
        assertFalse(AccionAbrirCofre.formatoValido("ORBITA-"));
        assertFalse(AccionAbrirCofre.formatoValido("A-B-C-D"));
        assertFalse(AccionAbrirCofre.formatoValido(AccionAbrirCofre.normalizar(" orbita-7392")));
        assertFalse(AccionAbrirCofre.formatoValido(null));
    }
}
