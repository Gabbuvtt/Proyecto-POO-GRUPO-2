package api;

import modelo.Coordenada;
import modelo.EstadoDePartida;
import modelo.Participante;
import modelo.Partida;
import modelo.TipoDeTerreno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Convierte respuestas JSON de ejemplo de la API en partidas, sin red. */
class RepositorioPartidasTest {

    /** Cliente falso: responde con textos fijos y guarda lo que se le envia. */
    private static class ClienteFalso extends ClienteApi {
        HashMap<String, String> respuestas = new HashMap<>();
        ArrayList<String> enviados = new ArrayList<>();
        int fallosDeRed = 0;

        ClienteFalso() {
            super("http://localhost:8000/api/v1");
        }

        @Override
        public String get(String ruta) {
            if (!respuestas.containsKey(ruta)) {
                throw new IllegalStateException("No se pudo establecer comunicacion con la API.");
            }
            return respuestas.get(ruta);
        }

        @Override
        public String post(String ruta, String cuerpoJson) {
            enviados.add(cuerpoJson);
            if (fallosDeRed > 0) {
                fallosDeRed--;
                throw new IllegalStateException("No se pudo establecer comunicacion con la API.");
            }
            return respuestas.get(ruta);
        }

        @Override
        public String put(String ruta, String cuerpoJson) {
            enviados.add(cuerpoJson);
            return "{\"partidaId\":\"EX-RED-01\",\"revision\":6,\"guardado\":true}";
        }
    }

    private static final String JUGADORES = """
            [{"jugadorId":"J1","nombre":"Ana","alias":"ana_ucab"},
             {"jugadorId":"J2","nombre":"Luis","alias":"luis_ucab"}]
            """;

    // C3 campamento, D2 cofre, templos en B1, B3, C4 y A5
    private static final String TERRENO = """
            [["S","T","S","S","P"],
             ["S","S","S","X","P"],
             ["S","T","C","S","S"],
             ["P","S","T","S","S"],
             ["T","S","S","P","S"]]
            """;

    // partida recien creada: empieza Luis porque va primero en el orden
    private static final String PARTIDA_NUEVA = """
            {"partidaId":"EX-RED-01","terreno":%s,"participantes":["J1","J2"],
             "orden":["J2","J1"],"campamento":"C3","cofre":"D2","revision":1,
             "estado":{"estado":"EN_CURSO","ronda":1,"indiceJugadorActual":0,"accionesRestantes":3,
               "jugadores":[
                 {"jugadorId":"J2","posicion":"C3","puntos":0,"inventario":[],"entregadas":[]},
                 {"jugadorId":"J1","posicion":"C3","puntos":0,"inventario":[],"entregadas":[]}],
               "templosExplorados":[],"pistas":[],"cofreAbierto":false,"ganadores":[]}}
            """.formatted(TERRENO);

    // ronda 2, turno de Luis con 1 accion
    private static final String PARTIDA_MITAD_TURNO = """
            {"partidaId":"EX-RED-01","terreno":%s,"participantes":["J1","J2"],
             "orden":["J1","J2"],"campamento":"C3","cofre":"D2","revision":5,
             "estado":{"estado":"EN_CURSO","ronda":2,"indiceJugadorActual":1,"accionesRestantes":1,
               "jugadores":[
                 {"jugadorId":"J1","posicion":"B2","puntos":0,"segundosConsumidos":70,
                  "inventario":[{"reliquiaId":"REL-01","valor":3}],"entregadas":[]},
                 {"jugadorId":"J2","posicion":"C2","puntos":2,"segundosConsumidos":35,
                  "inventario":[],"entregadas":[{"reliquiaId":"REL-02","valor":2}]}],
               "templosExplorados":["B3","C4"],
               "pistas":[{"indice":1,"fragmento":"ORBITA"},{"indice":2,"fragmento":"7392"}],
               "cofreAbierto":false,"ganadores":[]}}
            """.formatted(TERRENO);

    private ClienteFalso cliente;
    private RepositorioPartidas repositorio;

    @BeforeEach
    void preparar() {
        cliente = new ClienteFalso();
        cliente.respuestas.put("/jugadores", JUGADORES);
        repositorio = new RepositorioPartidas(cliente);
    }

    @Test
    void cargaUnaPartidaNuevaConElTerrenoYElOrden() {
        cliente.respuestas.put("/partidas/EX-RED-01", PARTIDA_NUEVA);
        Partida partida = repositorio.cargarPartida("EX-RED-01");

        assertEquals(1, partida.getRondaActual());
        assertEquals(3, partida.getAccionesRestantes());
        assertEquals(EstadoDePartida.EN_CURSO, partida.getEstado());
        assertEquals("Luis", partida.getTurnoActual().getJugador().getNombre());
        assertEquals("C3", partida.getParticipante2().getPosicion().getTexto());
        assertEquals(TipoDeTerreno.CAMPAMENTO,
                partida.getTablero().obtenerCasilla(new Coordenada("C3")).getTipo());
        assertEquals(TipoDeTerreno.COFRE,
                partida.getTablero().obtenerCasilla(new Coordenada("D2")).getTipo());
        assertEquals(TipoDeTerreno.TEMPLO,
                partida.getTablero().obtenerCasilla(new Coordenada("A5")).getTipo());
    }

    @Test
    void cargaUnaPartidaAMitadDeTurnoSinReiniciarlo() {
        cliente.respuestas.put("/partidas/EX-RED-01", PARTIDA_MITAD_TURNO);
        Partida partida = repositorio.cargarPartida("EX-RED-01");
        Participante ana = partida.getParticipante1();
        Participante luis = partida.getParticipante2();

        assertEquals(2, partida.getRondaActual());
        assertEquals(1, partida.getAccionesRestantes());
        assertEquals(1, partida.getIndiceJugadorActual());
        assertEquals("Luis", partida.getTurnoActual().getJugador().getNombre());

        assertEquals("B2", ana.getPosicion().getTexto());
        assertEquals("REL-01", ana.getInventario().getReliquias().get(0).getReliquiaId());
        assertEquals(70, ana.getSegundosConsumidos());
        assertEquals(2, luis.getPuntos());
        assertEquals(1, luis.getEntregadas().size());
        assertEquals(35, luis.getSegundosConsumidos());

        assertTrue(partida.yaFueExplorado(new Coordenada("B3")));
        assertTrue(partida.yaFueExplorado(new Coordenada("C4")));
        assertEquals(2, partida.getPistasDescubiertas().size());
        assertEquals("7392", partida.getPistasDescubiertas().get(1).getFragmento());
    }

    @Test
    void alGuardarDespuesDeCargarUsaLaRevisionGuardada() {
        cliente.respuestas.put("/partidas/EX-RED-01", PARTIDA_MITAD_TURNO);
        Partida partida = repositorio.cargarPartida("EX-RED-01");

        assertTrue(repositorio.guardar(partida));
        String cuerpo = cliente.enviados.get(0);
        assertTrue(cuerpo.contains("\"revisionBase\":5"));
        assertTrue(cuerpo.contains("\"segundosConsumidos\":70"));
    }

    @Test
    void laSaludSoloSirveConContrato2() {
        assertFalse(repositorio.apiDisponible()); // sin respuesta

        cliente.respuestas.put("/salud", "{\"disponible\":true,\"versionContrato\":1}");
        assertFalse(repositorio.apiDisponible());

        cliente.respuestas.put("/salud", "{\"disponible\":true,\"versionContrato\":2}");
        assertTrue(repositorio.apiDisponible());
    }

    @Test
    void crearPartidaReintentaConElMismoRequestId() {
        cliente.respuestas.put("/partidas", "{\"partidaId\":\"EX-RED-02\"}");
        cliente.fallosDeRed = 1;

        assertEquals("EX-RED-02", repositorio.crearPartida("J1", "J2"));
        assertEquals(2, cliente.enviados.size());
        assertEquals(cliente.enviados.get(0), cliente.enviados.get(1));
    }
}
