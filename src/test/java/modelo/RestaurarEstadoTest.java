package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** CA09: restaurar una partida guardada a mitad de turno y seguir jugando. */
class RestaurarEstadoTest {

    // mapa del ejemplo del enunciado
    private Tablero crearTablero() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("C3"), TipoDeTerreno.CAMPAMENTO);
        tablero.colocarTerreno(new Coordenada("D2"), TipoDeTerreno.COFRE);
        tablero.colocarTerreno(new Coordenada("B1"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("B3"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("C4"), TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(new Coordenada("A5"), TipoDeTerreno.TEMPLO);
        return tablero;
    }

    private Partida crearPartida() {
        Coordenada campamento = new Coordenada("C3");
        Participante ana = new Participante(new Jugador("J1", "Ana", "ana"), campamento);
        Participante luis = new Participante(new Jugador("J2", "Luis", "luis"), campamento);
        Partida partida = new Partida("EX-RED-01", crearTablero(), ana, luis);

        partida.esconderReliquia(new Coordenada("B3"), new Reliquia("REL-01", 3));
        partida.esconderReliquia(new Coordenada("C4"), new Reliquia("REL-02", 2));
        partida.esconderReliquia(new Coordenada("B1"), new Reliquia("REL-03", 2));
        partida.esconderReliquia(new Coordenada("A5"), new Reliquia("REL-04", 1));
        partida.esconderPista(new Coordenada("B3"), new Pista(1, "ORBITA"));
        partida.esconderPista(new Coordenada("C4"), new Pista(2, "7392"));
        return partida;
    }

    private void mover(Partida partida, String destino) {
        assertEquals(ResultadoAccion.OK, partida.ejecutarAccion(new AccionMover(new Coordenada(destino))));
    }

    private void ejecutar(Partida partida, AccionJuego accion) {
        assertEquals(ResultadoAccion.OK, partida.ejecutarAccion(accion));
    }

    // juega hasta la ronda 2 con Luis a mitad de turno
    private Partida jugarHastaMitadDeTurno() {
        Partida partida = crearPartida();

        // ronda 1: Ana explora B3 y sigue hacia B2
        mover(partida, "B3");
        ejecutar(partida, new AccionExplorar());
        mover(partida, "B2");
        partida.getParticipante1().sumarSegundos(40);

        // ronda 1: Luis explora C4 y vuelve
        mover(partida, "C4");
        ejecutar(partida, new AccionExplorar());
        mover(partida, "C3");
        partida.getParticipante2().sumarSegundos(25);

        // ronda 2: Ana explora B1 y se queda con el inventario lleno
        mover(partida, "B1");
        ejecutar(partida, new AccionExplorar());
        mover(partida, "B2");
        partida.getParticipante1().sumarSegundos(30);

        // ronda 2: Luis entrega y se mueve, le queda 1 accion
        ejecutar(partida, new AccionEntregar());
        mover(partida, "C2");
        partida.getParticipante2().sumarSegundos(10);
        return partida;
    }

    // copia los datos de un participante como lo haria el snapshot
    private void copiarParticipante(Participante origen, Participante destino) {
        destino.setPosicion(new Coordenada(origen.getPosicion().getTexto()));
        ArrayList<Reliquia> mochila = origen.getInventario().getReliquias();
        for (int i = 0; i < mochila.size(); i++) {
            destino.getInventario().agregarReliquia(mochila.get(i));
        }
        destino.restaurarPuntosYEntregadas(origen.getPuntos(), origen.getEntregadas());
        destino.sumarSegundos(origen.getSegundosConsumidos());
    }

    private Partida restaurarCopia(Partida original) {
        Partida copia = crearPartida();
        copiarParticipante(original.getParticipante1(), copia.getParticipante1());
        copiarParticipante(original.getParticipante2(), copia.getParticipante2());

        ArrayList<String> templos = original.getTemplosExplorados();
        for (int i = 0; i < templos.size(); i++) {
            copia.marcarComoExplorado(new Coordenada(templos.get(i)));
        }
        ArrayList<Pista> pistas = original.getPistasDescubiertas();
        for (int i = 0; i < pistas.size(); i++) {
            copia.agregarPistaDescubierta(pistas.get(i));
        }

        copia.restaurarEstado(original.getRondaActual(), original.getIndiceJugadorActual(),
                original.getAccionesRestantes(), original.getEstado(), original.estaCofreAbierto());
        return copia;
    }

    private ArrayList<String> idsDe(ArrayList<Reliquia> reliquias) {
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < reliquias.size(); i++) {
            ids.add(reliquias.get(i).getReliquiaId());
        }
        return ids;
    }

    private void compararParticipantes(Participante esperado, Participante real) {
        assertEquals(esperado.getJugadorId(), real.getJugadorId());
        assertEquals(esperado.getPosicion().getTexto(), real.getPosicion().getTexto());
        assertEquals(esperado.getPuntos(), real.getPuntos());
        assertEquals(idsDe(esperado.getInventario().getReliquias()), idsDe(real.getInventario().getReliquias()));
        assertEquals(idsDe(esperado.getEntregadas()), idsDe(real.getEntregadas()));
        assertEquals(esperado.getSegundosConsumidos(), real.getSegundosConsumidos());
    }

    @Test
    void restauraTodoAMitadDeTurno() {
        Partida original = jugarHastaMitadDeTurno();
        Partida copia = restaurarCopia(original);

        // estado esperado segun lo jugado
        assertEquals(2, copia.getRondaActual());
        assertEquals(1, copia.getIndiceJugadorActual());
        assertEquals(1, copia.getAccionesRestantes());
        assertEquals(EstadoDePartida.EN_CURSO, copia.getEstado());
        assertFalse(copia.estaCofreAbierto());
        assertEquals(2, copia.getPistasDescubiertas().size());
        assertEquals(3, copia.getTemplosExplorados().size());

        // todo coincide con la partida original
        assertEquals(original.getRondaActual(), copia.getRondaActual());
        assertEquals(original.getIndiceJugadorActual(), copia.getIndiceJugadorActual());
        assertEquals(original.getAccionesRestantes(), copia.getAccionesRestantes());
        assertEquals(original.getEstado(), copia.getEstado());
        assertEquals(original.getTurnoActual().getJugadorId(), copia.getTurnoActual().getJugadorId());
        compararParticipantes(original.getParticipante1(), copia.getParticipante1());
        compararParticipantes(original.getParticipante2(), copia.getParticipante2());

        ArrayList<String> templosOriginal = original.getTemplosExplorados();
        for (int i = 0; i < templosOriginal.size(); i++) {
            assertTrue(copia.yaFueExplorado(new Coordenada(templosOriginal.get(i))));
        }
        assertEquals("ORBITA", copia.getPistasDescubiertas().get(0).getFragmento());
        assertEquals("7392", copia.getPistasDescubiertas().get(1).getFragmento());
        assertEquals(2, copia.getParticipante2().getPuntos());
        assertEquals(2, copia.getParticipante1().getInventario().getReliquias().size());
    }

    @Test
    void seSigueJugandoSinReiniciarAcciones() {
        Partida copia = restaurarCopia(jugarHastaMitadDeTurno());

        // a Luis solo le queda 1 accion, no puede entrar a un pantano
        copia.getTablero().colocarTerreno(new Coordenada("C1"), TipoDeTerreno.PANTANO);
        assertEquals(ResultadoAccion.SIN_ACCIONES,
                copia.ejecutarAccion(new AccionMover(new Coordenada("C1"))));
        assertEquals(1, copia.getAccionesRestantes());

        // con su ultima accion vuelve a C3 y empieza la ronda 3 con Ana
        mover(copia, "C3");
        assertEquals(3, copia.getRondaActual());
        assertEquals(0, copia.getIndiceJugadorActual());
        assertEquals(ReglasDelJuego.ACCIONES_POR_TURNO, copia.getAccionesRestantes());

        // B1 sigue explorado despues de restaurar
        mover(copia, "B1");
        assertEquals(ResultadoAccion.TEMPLO_YA_EXPLORADO, copia.ejecutarAccion(new AccionExplorar()));
    }

    @Test
    void valoresInvalidosLanzanExcepcion() {
        Partida partida = crearPartida();
        int max = ReglasDelJuego.ACCIONES_POR_TURNO;

        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(0, 0, 1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(ReglasDelJuego.MAX_RONDAS + 1, 0, 1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(1, 2, 1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(1, -1, 1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(1, 0, -1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(1, 0, max + 1, EstadoDePartida.EN_CURSO, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.restaurarEstado(1, 0, 1, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> partida.getParticipante1().restaurarPuntosYEntregadas(-1, new ArrayList<>()));

        // si falla no cambia nada
        assertEquals(1, partida.getRondaActual());
        assertEquals(0, partida.getIndiceJugadorActual());
        assertEquals(max, partida.getAccionesRestantes());
    }
}
