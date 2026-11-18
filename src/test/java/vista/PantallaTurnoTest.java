package vista;

import modelo.Coordenada;
import modelo.Jugador;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.Reliquia;
import modelo.ResultadoAccion;
import modelo.Tablero;
import modelo.TipoDeTerreno;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PantallaTurnoTest {

    private final PrintStream salidaOriginal = System.out;
    private ByteArrayOutputStream captura;

    @BeforeEach
    void capturarSalida() {
        captura = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captura, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restaurarSalida() {
        System.setOut(salidaOriginal);
    }

    private String salida() {
        return captura.toString(StandardCharsets.UTF_8);
    }

    /** Partida de ejemplo: ronda 2, turno de Ana en C3 con REL-01 en el inventario. */
    private Partida partidaEjemplo() {
        Tablero tablero = new Tablero(5, 5);
        tablero.colocarTerreno(new Coordenada("C3"), TipoDeTerreno.CAMPAMENTO);
        Participante ana = new Participante(new Jugador("J1", "Ana", "ana_ucab"), new Coordenada("C3"));
        Participante luis = new Participante(new Jugador("J2", "Luis", "luis_ucab"), new Coordenada("D4"));
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 3));
        Partida partida = new Partida("EX-RED-01", tablero, ana, luis);
        partida.cambiarTurno();
        partida.cambiarTurno();
        return partida;
    }

    @Test
    void cabeceraMuestraPartidaRondaJugadorEInventario() {
        new VistaConsola(new Scanner("")).mostrarEstadoTurno(partidaEjemplo());

        String texto = salida();
        assertTrue(texto.contains("EX-RED-01 | Ronda 2 de 8"));
        assertTrue(texto.contains("Ana | Posición: C3 | Puntos: 0 | Acciones: 3"));
        assertTrue(texto.contains("Inventario: REL-01, valor 3"));
    }

    @Test
    void tableroDelTurnoMuestraColumnasYLeyenda() {
        new VistaConsola(new Scanner("")).mostrarTableroTurno(partidaEjemplo());

        String texto = salida();
        assertTrue(texto.contains("A    B    C    D    E"));
        assertTrue(texto.contains("1 = Ana | 2 = Luis"));
    }

    @Test
    void pistasSeMuestranPorSeparadoYNuncaUnidas() {
        Partida partida = partidaEjemplo();
        VistaConsola vista = new VistaConsola(new Scanner(""));

        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        vista.mostrarPistas(partida);
        assertTrue(salida().contains("Pista 1: ORBITA | Pista 2: (sin descubrir)"));

        captura.reset();
        partida.agregarPistaDescubierta(new Pista(2, "7392"));
        vista.mostrarPistas(partida);
        String texto = salida();
        assertTrue(texto.contains("Pista 1: ORBITA | Pista 2: 7392"));
        assertFalse(texto.contains("ORBITA-7392"));
        assertFalse(texto.contains("ORBITA7392"));
    }

    @Test
    void menuDelTurnoAceptaSoloDel0Al7() {
        assertEquals(7, new VistaConsola(new Scanner("7\n")).pedirOpcionTurno());
        assertEquals(3, new VistaConsola(new Scanner("8\n3\n")).pedirOpcionTurno());
    }

    @Test
    void coordenadaDestinoSeLeeSinImportarMayusculasNiEspacios() {
        Coordenada destino = new VistaConsola(new Scanner(" d4 \n")).pedirCoordenadaDestino();
        assertEquals("D4", destino.getTexto());
    }

    @Test
    void contrasenaDelCofreSeLeeTalCualSinQuitarEspacios() {
        VistaConsola vista = new VistaConsola(new Scanner(" ORBITA-7392 \n"));
        assertEquals(" ORBITA-7392 ", vista.pedirContrasenaCofre(partidaEjemplo()));
    }

    @Test
    void pantallaDelCofreMuestraPistasYEstadoPeroNoLaContrasenaUnida() {
        Partida partida = partidaEjemplo();
        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        partida.agregarPistaDescubierta(new Pista(2, "7392"));

        new VistaConsola(new Scanner("x\n")).pedirContrasenaCofre(partida);

        String texto = salida();
        assertTrue(texto.contains("Pista 1: ORBITA | Pista 2: 7392"));
        assertTrue(texto.contains("Ana en C3 | Cofre cerrado | Acciones: 3"));
        assertFalse(texto.contains("ORBITA-7392"));
    }

    @Test
    void cofreAbiertoAvisaValorYQueHayQueEntregarla() {
        new VistaConsola(new Scanner("")).mostrarCofreAbierto(1);

        String texto = salida();
        assertTrue(texto.contains("Cofre abierto. REL-ESPECIAL incorporada al inventario."));
        assertTrue(texto.contains("Valor: 4 | Acciones restantes: 1"));
        assertTrue(texto.contains("Recuerde entregarla en el campamento."));
    }

    @Test
    void resultadoFallidoMuestraElMensajeYLasAccionesRestantes() {
        new VistaConsola(new Scanner("")).mostrarResultado(ResultadoAccion.FALLO_AUTENTICACION, 2);

        String texto = salida();
        assertTrue(texto.contains("No se pudo autenticar el archivo."));
        assertTrue(texto.contains("Acciones restantes: 2."));
    }

    @Test
    void pantallaFinalMuestraPuntosTiempoYGanador() {
        Partida partida = partidaEjemplo();
        partida.getParticipante1().entregarReliquias();
        partida.getParticipante1().sumarSegundos(95);
        partida.getParticipante2().sumarSegundos(80);

        new VistaConsola(new Scanner("")).mostrarFinDePartida(partida);

        String texto = salida();
        assertTrue(texto.contains("FIN DE LA PARTIDA"));
        assertTrue(texto.contains("Ana: 3 puntos, 1 reliquias entregadas, 95 s de juego"));
        assertTrue(texto.contains("Luis: 0 puntos, 0 reliquias entregadas, 80 s de juego"));
        assertTrue(texto.contains("Ganador: Ana"));
    }
}
