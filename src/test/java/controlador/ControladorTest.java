package controlador;

import modelo.Coordenada;
import modelo.EstadoDePartida;
import modelo.Jugador;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.Reliquia;
import modelo.Tablero;
import modelo.TipoDeTerreno;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import servicio.PaqueteCofre;
import servicio.ServicioCriptografico;
import vista.VistaConsola;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorTest {

    /** Almacen falso: cuenta los guardados y puede fingir que la API falla. */
    private static class AlmacenFalso implements AlmacenPartida {
        int guardados = 0;
        boolean fallar = false;

        boolean conflicto = false;

        @Override
        public boolean guardar(Partida partida) {
            if (conflicto) {
                throw new ConflictoVersionException("La partida tiene una version mas nueva en la API.");
            }
            if (fallar) {
                return false;
            }
            guardados++;
            return true;
        }

        @Override
        public PaqueteCofre obtenerPaqueteCofre(String partidaId) {
            return null;
        }

        @Override
        public ContenidoTemplo obtenerContenidoTemplo(String partidaId, String coordenada) {
            if (fallar) {
                throw new IllegalStateException("La partida no existe");
            }
            return new ContenidoTemplo(new Reliquia("REL-01", 2), new Pista(1, "ORBITA"));
        }

        boolean disponible = true;
        boolean aliasRepetido = false;
        int partidasCreadas = 0;
        Partida partidaGuardada;

        @Override
        public boolean apiDisponible() {
            return disponible;
        }

        @Override
        public String registrarJugador(String nombre, String alias) {
            if (aliasRepetido) {
                throw new IllegalStateException("Ya existe un jugador con el alias " + alias);
            }
            return "J3";
        }

        @Override
        public Jugador buscarJugadorPorAlias(String alias) {
            if (alias.equals("nadie")) {
                return null;
            }
            return new Jugador("ID-" + alias, alias, alias);
        }

        @Override
        public String crearPartida(String jugador1, String jugador2) {
            partidasCreadas++;
            return partidaGuardada.getPartidaId();
        }

        @Override
        public ArrayList<ResumenPartida> listarPartidas(String jugadorId) {
            ArrayList<ResumenPartida> lista = new ArrayList<>();
            lista.add(new ResumenPartida(partidaGuardada.getPartidaId(), "ana_ucab, luis_ucab", "EN_CURSO", 1));
            return lista;
        }

        @Override
        public Partida cargarPartida(String partidaId) {
            return partidaGuardada;
        }

        @Override
        public EstadisticasJugador obtenerEstadisticas(String jugadorId) {
            return new EstadisticasJugador(1, 1, 8, 3);
        }
    }

    private final PrintStream salidaOriginal = System.out;
    private ByteArrayOutputStream captura;
    private long[] horas = {0};
    private int llamadasAlReloj = 0;
    private AlmacenFalso almacen;

    @BeforeEach
    void preparar() {
        captura = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captura, true, StandardCharsets.UTF_8));
        horas = new long[]{0};
        llamadasAlReloj = 0;
        almacen = new AlmacenFalso();
    }

    @AfterEach
    void restaurarSalida() {
        System.setOut(salidaOriginal);
    }

    private String salida() {
        return captura.toString(StandardCharsets.UTF_8);
    }

    /** Partida nueva: Ana en C3 (campamento) y Luis en D4. Empieza Ana, ronda 1. */
    private Partida partidaNueva() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("C3"), TipoDeTerreno.CAMPAMENTO);
        Participante ana = new Participante(new Jugador("J1", "Ana", "ana_ucab"), new Coordenada("C3"));
        Participante luis = new Participante(new Jugador("J2", "Luis", "luis_ucab"), new Coordenada("D4"));
        return new Partida("EX-RED-01", tablero, ana, luis);
    }

    /** Reloj falso: cada vez que el controlador mira la hora, devuelve el siguiente valor (en milisegundos). */
    private long reloj() {
        long hora = horas[Math.min(llamadasAlReloj, horas.length - 1)];
        llamadasAlReloj++;
        return hora;
    }

    /** Crea el controlador con las respuestas del jugador y el reloj falso. */
    private Controlador controlador(String teclas) {
        return new Controlador(new VistaConsola(new Scanner(teclas)), new ServicioCriptografico(), almacen,
                this::reloj);
    }

    @Test
    void terminarElTurnoPasaElTurnoSumaElTiempoYGuarda() {
        Partida partida = partidaNueva();
        Participante ana = partida.getTurnoActual();

        // horas que ve el controlador: empieza en 0, Ana termina a los 42 s, Luis guarda y sale a los 50 s
        horas = new long[]{0, 42_000, 42_000, 50_000};
        // el jugador elige 7 (terminar turno) y luego 0 (guardar y salir)
        controlador("7\n0\n").jugar(partida);

        assertSame(partida.getParticipante2(), partida.getTurnoActual());
        assertEquals(42, ana.getSegundosConsumidos());
        assertEquals(8, partida.getParticipante2().getSegundosConsumidos());
        assertEquals(2, almacen.guardados); // uno al cerrar el turno y otro al guardar y salir
    }

    @Test
    void guardarYSalirNoSaleSiLaApiNoConfirma() {
        almacen.fallar = true;
        Partida partida = partidaNueva();

        // intenta salir (falla), vuelve al menu y sale con otra opcion de consulta + otro intento
        Controlador controlador = controlador("0\n5\n0\n");
        try {
            controlador.jugar(partida);
        } catch (IllegalStateException e) {
            // se acabaron las teclas: justo lo que queremos, el juego seguia pidiendo opciones
        }

        String texto = salida();
        assertTrue(texto.contains("por eso no se cerró el juego"));
        assertEquals(0, almacen.guardados);
        assertTrue(texto.indexOf("Opción:") != texto.lastIndexOf("Opción:")); // pidio mas de una vez
    }

    @Test
    void guardarYSalirSiSaleCuandoLaApiConfirma() {
        controlador("0\n").jugar(partidaNueva());

        assertEquals(1, almacen.guardados);
        assertTrue(salida().contains("Partida guardada. ¡Hasta pronto!"));
    }

    @Test
    void gastarLaUltimaAccionCierraElTurnoYGuardaSolo() {
        Partida partida = partidaNueva();
        // Ana mueve 3 veces: C3 -> C2 -> C1 -> D1 (sendero por defecto, 1 punto cada una) y luego sale
        Controlador controlador = controlador("1\nC2\n1\nC1\n1\nD1\n0\n");
        controlador.jugar(partida);

        assertSame(partida.getParticipante2(), partida.getTurnoActual());
        assertEquals(2, almacen.guardados); // cierre de turno + guardar y salir
        assertTrue(salida().contains("Acciones restantes: 0."));
    }

    @Test
    void alTerminarLaOctavaRondaMuestraElFinYGuardaElEstadoFinalizado() {
        Partida partida = partidaNueva();
        // se avanzan 15 cambios de turno: queda el turno de Luis en la ronda 8
        for (int i = 0; i < 15; i++) {
            partida.cambiarTurno();
        }
        // Luis termina su turno y la partida acaba sola
        controlador("7\n").jugar(partida);

        assertEquals(EstadoDePartida.FINALIZADA, partida.getEstado());
        assertEquals(1, almacen.guardados);
        String texto = salida();
        assertTrue(texto.contains("FIN DE LA PARTIDA"));
        assertTrue(texto.contains("Victoria compartida."));
    }

    @Test
    void sinEstarEnElCofreNoPideContrasenaNiCobraAccion() {
        Partida partida = partidaNueva();
        controlador("4\n0\n").jugar(partida);

        assertEquals(3, partida.getAccionesRestantes());
        assertTrue(salida().contains("Debes estar en el cofre (X)"));
    }

    @Test
    void elTiempoSeAcumulaEnVariosGuardadosSinPerderSegundos() {
        Partida partida = partidaNueva();
        Participante ana = partida.getTurnoActual();
        // el controlador ve 0 s al empezar, y luego 1,5 s, 2,6 s y 3,1 s en cada guardado
        horas = new long[]{0, 1_500, 2_600, 3_100};
        controlador("6\n6\n0\n").jugar(partida);

        assertEquals(3, ana.getSegundosConsumidos());
        assertEquals(3, almacen.guardados);
    }

    /** Partida con un templo en B3 donde esta parada Ana. */
    private Partida partidaEnTemplo() {
        Partida partida = partidaNueva();
        partida.getTablero().colocarTerreno(new Coordenada("B3"), TipoDeTerreno.TEMPLO);
        partida.getTurnoActual().setPosicion(new Coordenada("B3"));
        return partida;
    }

    @Test
    void explorarUnTemploTraeLaReliquiaYLaPistaDeLaApi() {
        Partida partida = partidaEnTemplo();
        Participante ana = partida.getTurnoActual();
        controlador("2\n0\n").jugar(partida);

        assertEquals(1, ana.getInventario().getReliquias().size());
        assertEquals("REL-01", ana.getInventario().getReliquias().get(0).getReliquiaId());
        assertEquals("ORBITA", partida.getPistasDescubiertas().get(0).getFragmento());
        assertEquals(2, partida.getAccionesRestantes());
    }

    @Test
    void siLaApiFallaAlExplorarNoSeGastaLaAccion() {
        almacen.fallar = true;
        Partida partida = partidaEnTemplo();
        Controlador controlador = controlador("2\n");
        try {
            controlador.jugar(partida);
        } catch (IllegalStateException e) {
            // se acabaron las teclas
        }

        assertEquals(3, partida.getAccionesRestantes());
        assertEquals(0, partida.getTurnoActual().getInventario().getReliquias().size());
        assertTrue(salida().contains("No se pudo consultar el templo en la API: La partida no existe"));
    }

    @Test
    void unConflictoDeVersionesSeExplicaYBloqueaLasAcciones() {
        almacen.conflicto = true;
        Partida partida = partidaNueva();
        // guarda (conflicto) e intenta mover, que queda bloqueado
        Controlador controlador = controlador("6\n1\n");
        try {
            controlador.jugar(partida);
        } catch (IllegalStateException e) {
            // se acabaron las teclas
        }

        String texto = salida();
        assertTrue(texto.contains("Error: Conflicto de versiones: La partida tiene una version mas nueva en la API."));
        assertTrue(texto.contains("Usa la opcion 0 para volver al menu"));
        assertFalse(texto.contains("Ctrl+C"));
        assertTrue(texto.contains("No puedes realizar acciones mientras haya un guardado pendiente."));
        assertEquals(3, partida.getAccionesRestantes());
    }

    @Test
    void conConflictoGuardarYSalirVuelveAlMenuSinGuardar() {
        almacen.conflicto = true;
        // guarda (conflicto) y despues 0: tiene que salir sin pedir mas teclas
        controlador("6\n0\n").jugar(partidaNueva());

        String texto = salida();
        assertTrue(texto.contains("Se vuelve al menu sin guardar. Los cambios desde el ultimo guardado se pierden."));
        assertFalse(texto.contains("Partida guardada. ¡Hasta pronto!"));
        assertEquals(0, almacen.guardados);
    }

    @Test
    void siElPrimerGuardarYSalirDaConflictoTambienSale() {
        almacen.conflicto = true;
        controlador("0\n").jugar(partidaNueva());

        assertTrue(salida().contains("Se vuelve al menu sin guardar."));
    }

    @Test
    void laAyudaDescifraElVectorDelAnexoB() {
        controlador("5\n1\n0\n").iniciar();

        String texto = salida();
        assertTrue(texto.contains("Partida: EX-RED-01 | Cofre: COFRE-01"));
        assertTrue(texto.contains("Texto descifrado: {\"partidaId\":\"EX-RED-01\",\"cofreId\":\"COFRE-01\","
                + "\"reliquiaId\":\"REL-ESPECIAL\",\"valor\":4}"));
    }

    @Test
    void sinApiMuestraUnMensajeYTermina() {
        almacen.disponible = false;
        controlador("").iniciar();

        assertTrue(salida().contains("La API no esta disponible"));
    }

    @Test
    void registrarJugadorMuestraSuId() {
        controlador("1\nAna Perez\nana_ucab\n0\n").iniciar();

        assertTrue(salida().contains("Jugador creado: J3"));
    }

    @Test
    void aliasRepetidoMuestraElErrorYVuelveAlMenu() {
        almacen.aliasRepetido = true;
        controlador("1\nAna Perez\nana_ucab\n0\n").iniciar();

        String texto = salida();
        assertTrue(texto.contains("Error: Ya existe un jugador con el alias ana_ucab"));
        assertTrue(texto.indexOf("1. Registrar jugador") != texto.lastIndexOf("1. Registrar jugador"));
    }

    @Test
    void nuevaPartidaRechazaAlMismoJugadorDosVeces() {
        controlador("2\nana_ucab\nana_ucab\n0\n").iniciar();

        assertTrue(salida().contains("Los dos jugadores deben ser distintos."));
        assertEquals(0, almacen.partidasCreadas);
    }

    @Test
    void nuevaPartidaMuestraElOrdenYEmpiezaAJugar() {
        almacen.partidaGuardada = partidaNueva();
        // crea la partida, guarda y sale del juego, y sale del menu
        controlador("2\nana_ucab\nluis_ucab\n0\n0\n").iniciar();

        String texto = salida();
        assertEquals(1, almacen.partidasCreadas);
        assertTrue(texto.contains("Partida: EX-RED-01 | Campamento: C3"));
        assertTrue(texto.contains("Orden sorteado: Ana, Luis | Ronda: 1 de 8"));
        assertTrue(texto.contains("Partida guardada. ¡Hasta pronto!"));
    }

    @Test
    void cargarPartidaSigueConElMismoTurno() {
        Partida partida = partidaNueva();
        partida.consumirAcciones(2);
        almacen.partidaGuardada = partida;
        // elige la partida 1, guarda y sale, y sale del menu
        controlador("3\nana_ucab\n1\n0\n0\n").iniciar();

        String texto = salida();
        assertTrue(texto.contains("EX-RED-01 | ana_ucab, luis_ucab | EN_CURSO | Ronda 1"));
        assertTrue(texto.contains("Acciones: 1"));
        assertEquals(1, partida.getAccionesRestantes());
    }

    @Test
    void estadisticasMuestraLosDatosDelJugador() {
        controlador("4\nana_ucab\n0\n").iniciar();

        String texto = salida();
        assertTrue(texto.contains("ESTADÍSTICAS DE ANA_UCAB"));
        assertTrue(texto.contains("Victorias: 1"));
        assertTrue(texto.contains("Puntos acumulados: 8"));
    }

    @Test
    void aliasInexistenteAvisaYVuelveAlMenu() {
        controlador("4\nnadie\n0\n").iniciar();

        assertTrue(salida().contains("No existe un jugador con el alias nadie."));
    }
}