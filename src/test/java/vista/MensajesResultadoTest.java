package vista;

import modelo.ResultadoAccion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensajesResultadoTest {

    private final MensajesResultado mensajes = new MensajesResultado();

    @Test
    void todosLosResultadosTienenMensaje() {
        for (ResultadoAccion resultado : ResultadoAccion.values()) {
            assertFalse(mensajes.describir(resultado).isBlank(), "Falta mensaje para " + resultado);
        }
    }

    @Test
    void losMensajesDelEnunciadoSonLosEsperados() {
        assertEquals("No se pudo autenticar el archivo.", mensajes.describir(ResultadoAccion.FALLO_AUTENTICACION));
        assertEquals("Explorado y vacío.", mensajes.describir(ResultadoAccion.TEMPLO_YA_EXPLORADO));
        assertTrue(mensajes.describir(ResultadoAccion.INVENTARIO_LLENO).contains("Regrese al campamento"));
    }

    @Test
    void unResultadoNuloEsUnErrorDeProgramacion() {
        assertThrows(IllegalArgumentException.class, () -> mensajes.describir(null));
    }
}