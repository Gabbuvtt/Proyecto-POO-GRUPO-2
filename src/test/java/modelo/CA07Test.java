package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import servicio.PaqueteCofre;
import servicio.ServicioCriptografico;

/** CA07: el cofre exige espacio y da REL-ESPECIAL una sola vez, entregar en C suma y vacia. */
class CA07Test {

    private Partida partida;
    private Participante ana;
    private Participante luis;
    private final ServicioCriptografico servicio = new ServicioCriptografico();

    // mapa del ejemplo con las dos pistas ya descubiertas
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
        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        partida.agregarPistaDescubierta(new Pista(2, "7392"));
    }

    // vector del Anexo B, el mismo de CA06Test
    private ResultadoAccion abrirEn(String casilla) {
        partida.getTurnoActual().setPosicion(new Coordenada(casilla));
        PaqueteCofre paquete = new PaqueteCofre("EX-RED-01", "COFRE-01", "AES/GCM/NoPadding",
                "PBKDF2WithHmacSHA256", 100000, 256, CA06Test.SAL, CA06Test.NONCE, 128,
                "RVgtUkVELTAxfENPRlJFLTAx", CA06Test.CIFRADO);
        return partida.ejecutarAccion(new AccionAbrirCofre(paquete, "ORBITA-7392", servicio));
    }

    private ResultadoAccion entregarEn(String casilla) {
        partida.getTurnoActual().setPosicion(new Coordenada(casilla));
        return partida.ejecutarAccion(new AccionEntregar());
    }

    @Test
    void cofreConInventarioLlenoNoCobra() {
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));

        assertEquals(ResultadoAccion.INVENTARIO_LLENO, abrirEn("D2"));
        assertEquals(3, partida.getAccionesRestantes());
        assertFalse(partida.estaCofreAbierto());
        assertEquals(2, ana.getInventario().getReliquias().size());
    }

    // aunque Ana entregue la especial y vuelva, el cofre no da otra
    @Test
    void reliquiaEspecialSoloUnaVez() {
        assertEquals(ResultadoAccion.OK, abrirEn("D2"));
        assertEquals(ResultadoAccion.OK, entregarEn("C3"));
        assertEquals(4, ana.getPuntos());

        assertEquals(ResultadoAccion.COFRE_YA_ABIERTO, abrirEn("D2"));
        assertEquals(1, partida.getAccionesRestantes());
        assertTrue(ana.getInventario().estaVacio());

        partida.cambiarTurno();
        assertEquals(ResultadoAccion.COFRE_YA_ABIERTO, abrirEn("D2"));
        assertEquals(3, partida.getAccionesRestantes());
        assertTrue(luis.getInventario().estaVacio());
    }

    @Test
    void entregarEnCampamentoSumaYVacia() {
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));

        assertEquals(ResultadoAccion.OK, entregarEn("C3"));
        assertEquals(2, partida.getAccionesRestantes());
        assertEquals(5, ana.getPuntos());
        assertTrue(ana.getInventario().estaVacio());

        ArrayList<Reliquia> entregadas = ana.getEntregadas();
        assertEquals(2, entregadas.size());
        assertEquals("REL-01", entregadas.get(0).getReliquiaId());
        assertEquals("REL-02", entregadas.get(1).getReliquiaId());
    }

    // como en el ejemplo: Ana entrega 3 y luego 4, queda con 7
    @Test
    void entregasSeAcumulan() {
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));
        entregarEn("C3");
        ana.getInventario().agregarReliquia(new Reliquia("REL-ESPECIAL", 4));
        entregarEn("C3");

        assertEquals(7, ana.getPuntos());
        assertEquals(2, ana.getEntregadas().size());
    }

    @Test
    void entregarFueraDelCampamentoNoCobra() {
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));

        assertEquals(ResultadoAccion.NO_ESTA_EN_CAMPAMENTO, entregarEn("B3"));
        assertEquals(3, partida.getAccionesRestantes());
        assertEquals(0, ana.getPuntos());
        assertEquals(1, ana.getInventario().getReliquias().size());
        assertTrue(ana.getEntregadas().isEmpty());
    }

    @Test
    void entregarConInventarioVacioNoCobra() {
        assertEquals(ResultadoAccion.INVENTARIO_VACIO, entregarEn("C3"));
        assertEquals(3, partida.getAccionesRestantes());
        assertEquals(0, ana.getPuntos());
        assertTrue(ana.getEntregadas().isEmpty());
    }
}
