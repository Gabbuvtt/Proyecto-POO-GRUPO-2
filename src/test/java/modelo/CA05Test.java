package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CA05: dos pistas en templos distintos, visibles para ambos y sin ocupar inventario. */
class CA05Test {

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

    // ronda 1 del ejemplo, Ana revela la pista 1 y Luis la pista 2
    @Test
    void pistaDeUnJugadorLaVeElOtro() {
        explorarEn("B3");
        partida.cambiarTurno();

        // ya en el turno de Luis, antes de explorar
        ArrayList<Pista> vistasPorLuis = partida.getPistasDescubiertas();
        assertEquals(1, vistasPorLuis.size());
        assertEquals("ORBITA", vistasPorLuis.get(0).getFragmento());

        explorarEn("C4");
        partida.cambiarTurno();

        // de vuelta en el turno de Ana, en la ronda 2
        ArrayList<Pista> vistasPorAna = partida.getPistasDescubiertas();
        assertEquals(2, vistasPorAna.size());
        assertEquals(1, vistasPorAna.get(0).getIndice());
        assertEquals(2, vistasPorAna.get(1).getIndice());
        assertEquals("7392", vistasPorAna.get(1).getFragmento());
        assertTrue(partida.tienenTodasLasPistas());
    }

    @Test
    void temploSinPistaNoAgregaAlRegistro() {
        explorarEn("B1");
        assertTrue(partida.getPistasDescubiertas().isEmpty());
        assertFalse(partida.tienenTodasLasPistas());
    }

    // si las pistas ocuparan espacio, la segunda exploracion se rechazaria
    @Test
    void pistasNoOcupanInventario() {
        assertEquals(ResultadoAccion.OK, explorarEn("B3"));
        assertEquals(ResultadoAccion.OK, explorarEn("C4"));

        ArrayList<Reliquia> mochila = ana.getInventario().getReliquias();
        assertEquals(2, mochila.size());
        assertEquals("REL-01", mochila.get(0).getReliquiaId());
        assertEquals("REL-02", mochila.get(1).getReliquiaId());
        assertEquals(2, partida.getPistasDescubiertas().size());
    }
}
