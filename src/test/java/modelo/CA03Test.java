package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CA03: movimientos rechazados no cambian el estado y el pantano cuesta dos. */
class CA03Test {

    private Partida partida;
    private Participante ana;
    private Participante luis;

    // mapa del ejemplo, de los pantanos solo se usa D1 (ronda 7 del enunciado)
    @BeforeEach
    void prepararPartida() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("C3"), TipoDeTerreno.CAMPAMENTO);
        tablero.colocarTerreno(new Coordenada("D2"), TipoDeTerreno.COFRE);
        tablero.colocarTerreno(new Coordenada("B1"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("B3"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("C4"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("A5"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("D1"), TipoDeTerreno.PANTANO);

        Coordenada campamento = new Coordenada("C3");
        ana = new Participante(new Jugador("J1", "Ana", "ana"), campamento);
        luis = new Participante(new Jugador("J2", "Luis", "luis"), campamento);
        partida = new Partida("EX-RED-01", tablero, ana, luis);
    }

    private ResultadoAccion mover(String destino) {
        return partida.ejecutarAccion(new AccionMover(new Coordenada(destino)));
    }

    // revisa que Ana siga en su casilla, con su turno y sus acciones
    private void sinCambios(String posicion, int acciones) {
        assertEquals(posicion, ana.getPosicion().getTexto());
        assertEquals(acciones, partida.getAccionesRestantes());
        assertSame(ana, partida.getTurnoActual());
    }

    @Test
    void diagonalSeRechaza() {
        assertEquals(ResultadoAccion.NO_ES_VECINO, mover("D2"));
        sinCambios("C3", 3);
    }

    @Test
    void saltoDeDosCasillasSeRechaza() {
        assertEquals(ResultadoAccion.NO_ES_VECINO, mover("C5"));
        sinCambios("C3", 3);
    }

    @Test
    void fueraDelMapaSeRechaza() {
        ana.setPosicion(new Coordenada("E1"));
        assertEquals(ResultadoAccion.FUERA_DEL_MAPA, mover("F1"));
        assertEquals(ResultadoAccion.FUERA_DEL_MAPA, mover("E0"));
        sinCambios("E1", 3);
    }

    @Test
    void pantanoSinPresupuestoSeRechaza() {
        mover("D3");
        mover("D2");
        assertEquals(ResultadoAccion.SIN_ACCIONES, mover("D1"));
        sinCambios("D2", 1);
    }

    @Test
    void entrarAlPantanoCuestaDos() {
        ana.setPosicion(new Coordenada("E1"));
        assertEquals(ResultadoAccion.OK, mover("D1"));
        assertEquals("D1", ana.getPosicion().getTexto());
        assertEquals(1, partida.getAccionesRestantes());
    }

    // ronda 7 del ejemplo: E1 -> D1 (2) -> C1 (1) usa todo el turno
    @Test
    void salirDelPantanoASenderoCuestaUno() {
        ana.setPosicion(new Coordenada("E1"));
        mover("D1");
        assertEquals(ResultadoAccion.OK, mover("C1"));
        assertEquals("C1", ana.getPosicion().getTexto());
        assertSame(luis, partida.getTurnoActual());
        assertEquals(3, partida.getAccionesRestantes());
    }
}
