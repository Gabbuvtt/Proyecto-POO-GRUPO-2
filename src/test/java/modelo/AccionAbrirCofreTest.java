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

    @Test
    void comprobarCondicionesRevisaTodoAntesDePedirLaContrasena() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("D2"), TipoDeTerreno.COFRE);
        Participante ana = new Participante(new Jugador("J1", "Ana", "ana"), new Coordenada("C3"));
        Participante luis = new Participante(new Jugador("J2", "Luis", "luis"), new Coordenada("C3"));
        Partida partida = new Partida("EX-RED-01", tablero, ana, luis);

        assertEquals(ResultadoAccion.NO_ESTA_EN_COFRE, AccionAbrirCofre.comprobarCondiciones(partida));

        ana.setPosicion(new Coordenada("D2"));
        assertEquals(ResultadoAccion.FALTAN_PISTAS, AccionAbrirCofre.comprobarCondiciones(partida));

        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        partida.agregarPistaDescubierta(new Pista(2, "7392"));
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));
        assertEquals(ResultadoAccion.INVENTARIO_LLENO, AccionAbrirCofre.comprobarCondiciones(partida));

        ana.entregarReliquias();
        assertEquals(ResultadoAccion.OK, AccionAbrirCofre.comprobarCondiciones(partida));

        partida.abrirCofre();
        assertEquals(ResultadoAccion.COFRE_YA_ABIERTO, AccionAbrirCofre.comprobarCondiciones(partida));
        assertEquals(3, partida.getAccionesRestantes());
    }
}
