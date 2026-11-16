package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CA04: explorar y recoger en una accion, inventario lleno y templo ya explorado. */
class CA04Test {

    private Partida partida;
    private Participante ana;
    private Participante luis;

    // mapa y contenido de los templos del ejemplo del enunciado
    @BeforeEach
    void prepararPartida() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("C3"), TipoDeTerreno.CAMPAMENTO);
        tablero.colocarTerreno(new Coordenada("D2"), TipoDeTerreno.COFRE);
        tablero.colocarTerreno(new Coordenada("B1"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("B3"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("C4"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("A5"), TipoDeTerreno.TEMPLO);

        Coordenada campamento = new Coordenada("C3");
        ana = new Participante(new Jugador("J1", "Ana", "ana"), campamento);
        luis = new Participante(new Jugador("J2", "Luis", "luis"), campamento);
        partida = new Partida("EX-RED-01", tablero, ana, luis);

        partida.esconderReliquia(new Coordenada("B3"), new Reliquia("REL-01", 3));
        partida.esconderReliquia(new Coordenada("C4"), new Reliquia("REL-02", 2));
        partida.esconderReliquia(new Coordenada("B1"), new Reliquia("REL-03", 2));
        partida.esconderReliquia(new Coordenada("A5"), new Reliquia("REL-04", 1));
        partida.esconderPista(new Coordenada("B3"), new Pista(1, "ORBITA"));
        partida.esconderPista(new Coordenada("C4"), new Pista(2, "7392"));
    }

    private ResultadoAccion explorarEn(String templo) {
        partida.getTurnoActual().setPosicion(new Coordenada(templo));
        return partida.ejecutarAccion(new AccionExplorar());
    }

    // ronda 1 del ejemplo: Ana explora B3
    @Test
    void explorarRecogeYRegistraPistaPorUno() {
        assertEquals(ResultadoAccion.OK, explorarEn("B3"));
        assertEquals(2, partida.getAccionesRestantes());

        ArrayList<Reliquia> mochila = ana.getInventario().getReliquias();
        assertEquals(1, mochila.size());
        assertEquals("REL-01", mochila.get(0).getReliquiaId());
        assertEquals(3, mochila.get(0).getValor());

        ArrayList<Pista> pistas = partida.getPistasDescubiertas();
        assertEquals(1, pistas.size());
        assertEquals("ORBITA", pistas.get(0).getFragmento());
    }

    @Test
    void inventarioLlenoNoCobraNiRevelaNada() {
        ana.getInventario().agregarReliquia(new Reliquia("REL-X1", 1));
        ana.getInventario().agregarReliquia(new Reliquia("REL-X2", 1));

        assertEquals(ResultadoAccion.INVENTARIO_LLENO, explorarEn("C4"));
        assertEquals(3, partida.getAccionesRestantes());
        assertTrue(partida.getPistasDescubiertas().isEmpty());
        assertFalse(partida.yaFueExplorado(new Coordenada("C4")));
        assertEquals("REL-X1", ana.getInventario().getReliquias().get(0).getReliquiaId());
        assertEquals("REL-X2", ana.getInventario().getReliquias().get(1).getReliquiaId());

        // la reliquia sigue en el templo: tras entregar se puede recoger
        ana.entregarReliquias();
        assertEquals(ResultadoAccion.OK, explorarEn("C4"));
        assertEquals("REL-02", ana.getInventario().getReliquias().get(0).getReliquiaId());
        assertEquals("7392", partida.getPistasDescubiertas().get(0).getFragmento());
    }

    @Test
    void temploYaExploradoNoCobraNiDaOtraReliquia() {
        explorarEn("B3");
        assertEquals(ResultadoAccion.TEMPLO_YA_EXPLORADO, explorarEn("B3"));
        assertEquals(2, partida.getAccionesRestantes());
        assertEquals(1, ana.getInventario().getReliquias().size());
        assertEquals(1, partida.getPistasDescubiertas().size());

        // Luis tampoco recibe nada en el mismo templo
        partida.cambiarTurno();
        assertEquals(ResultadoAccion.TEMPLO_YA_EXPLORADO, explorarEn("B3"));
        assertEquals(3, partida.getAccionesRestantes());
        assertTrue(luis.getInventario().estaVacio());
    }
}
