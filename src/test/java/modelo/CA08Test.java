package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CA08: ocho rondas completas, reliquias sin entregar, desempate y victoria compartida. */
class CA08Test {

    private Partida partida;
    private Participante ana;
    private Participante luis;

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
    }

    // gasta las 3 acciones yendo y viniendo entre C3 y D3, el turno se cierra solo
    private void jugarTurno() {
        for (int i = 0; i < 3; i++) {
            Participante actual = partida.getTurnoActual();
            String destino = "D3";
            if (actual.getPosicion().getTexto().equals("D3")) {
                destino = "C3";
            }
            assertEquals(ResultadoAccion.OK, partida.ejecutarAccion(new AccionMover(new Coordenada(destino))));
        }
    }

    // 8 rondas de 2 turnos
    private void jugarOchoRondas() {
        for (int i = 0; i < 16; i++) {
            jugarTurno();
        }
    }

    private void entregar(Participante participante, Reliquia reliquia) {
        participante.getInventario().agregarReliquia(reliquia);
        participante.entregarReliquias();
    }

    @Test
    void ochoRondasDejanLaPartidaFinalizada() {
        for (int i = 0; i < 15; i++) {
            jugarTurno();
        }
        // antes del ultimo turno sigue en curso
        assertEquals(EstadoDePartida.EN_CURSO, partida.getEstado());
        assertEquals(8, partida.getRondaActual());
        assertSame(luis, partida.getTurnoActual());

        jugarTurno();
        assertEquals(EstadoDePartida.FINALIZADA, partida.getEstado());
        assertEquals(8, partida.getRondaActual());
        assertEquals(0, partida.getAccionesRestantes());
    }

    @Test
    void partidaFinalizadaNoDaMasAcciones() {
        jugarOchoRondas();
        String posicion = partida.getTurnoActual().getPosicion().getTexto();

        assertEquals(ResultadoAccion.PARTIDA_FINALIZADA,
                partida.ejecutarAccion(new AccionMover(new Coordenada("C2"))));
        assertEquals(ResultadoAccion.PARTIDA_FINALIZADA, partida.ejecutarAccion(new AccionEntregar()));
        partida.cambiarTurno();

        assertEquals(EstadoDePartida.FINALIZADA, partida.getEstado());
        assertEquals(0, partida.getAccionesRestantes());
        assertEquals(posicion, partida.getTurnoActual().getPosicion().getTexto());
    }

    // Ana carga 3 puntos en la mochila, pero solo cuentan los 3 entregados
    @Test
    void inventarioNoPuntua() {
        entregar(ana, new Reliquia("REL-01", 3));
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));
        ana.getInventario().agregarReliquia(new Reliquia("REL-04", 1));
        entregar(luis, new Reliquia("REL-ESPECIAL", 4));
        jugarOchoRondas();

        assertEquals(3, ana.getPuntos());
        ArrayList<Participante> ganadores = partida.obtenerGanadores();
        assertEquals(1, ganadores.size());
        assertSame(luis, ganadores.get(0));
    }

    // los dos con 4 puntos: Luis entrego dos reliquias y Ana una
    @Test
    void empateEnPuntosGanaQuienEntregoMas() {
        entregar(ana, new Reliquia("REL-ESPECIAL", 4));
        entregar(luis, new Reliquia("REL-02", 2));
        entregar(luis, new Reliquia("REL-03", 2));
        jugarOchoRondas();

        ArrayList<Participante> ganadores = partida.obtenerGanadores();
        assertEquals(1, ganadores.size());
        assertSame(luis, ganadores.get(0));
    }

    @Test
    void empateTotalLosDosGanan() {
        entregar(ana, new Reliquia("REL-01", 3));
        entregar(ana, new Reliquia("REL-04", 1));
        entregar(luis, new Reliquia("REL-02", 2));
        entregar(luis, new Reliquia("REL-03", 2));
        jugarOchoRondas();

        ArrayList<Participante> ganadores = partida.obtenerGanadores();
        assertEquals(2, ganadores.size());
        assertTrue(ganadores.contains(ana));
        assertTrue(ganadores.contains(luis));
    }
}
