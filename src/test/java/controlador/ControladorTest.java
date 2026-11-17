package controlador;

import modelo.Coordenada;
import modelo.EstadoDePartida;
import modelo.Jugador;
import modelo.Participante;
import modelo.Partida;
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
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorTest {

    /** Almacen falso: cuenta los guardados y puede fingir que la API falla. */
    private static class AlmacenFalso implements AlmacenPartida {
        int guardados = 0;
        boolean fallar = false;

        @Override
        public boolean guardar(Partida partida) {
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
}