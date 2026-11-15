package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import servicio.PaqueteCofre;
import servicio.ServicioCriptografico;

/** CA06: abrir el cofre con el vector del Anexo B y revisar cuando cobra. */
class CA06Test {

    static final String SAL = "xBxRniX+mjbP3gI6qy93zA==";
    static final String NONCE = "BVmBDNDCL0Gw5CM6";
    static final String CIFRADO =
            "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0"
                    + "pikC+bkl1DmSNPrLlES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ==";

    private Partida partida;
    private Participante ana;
    private Participante luis;
    private final ServicioCriptografico servicio = new ServicioCriptografico();

    // mapa del ejemplo: Ana y Luis parados en el cofre D2
    @BeforeEach
    void prepararPartida() {
        Tablero tablero = new Tablero(5, 5);
        Coordenada cofre = new Coordenada(1, 3);
        tablero.colocarTerreno(cofre, TipoDeTerreno.COFRE);
        ana = new Participante(new Jugador("J1", "Ana", "ana"), cofre);
        luis = new Participante(new Jugador("J2", "Luis", "luis"), cofre);
        partida = new Partida("EX-RED-01", tablero, ana, luis);
        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        partida.agregarPistaDescubierta(new Pista(2, "7392"));
    }

    private PaqueteCofre paquete(String cifrado) {
        return new PaqueteCofre("EX-RED-01", "COFRE-01", "AES/GCM/NoPadding", "PBKDF2WithHmacSHA256",
                100000, 256, SAL, NONCE, 128, "RVgtUkVELTAxfENPRlJFLTAx", cifrado);
    }

    private ResultadoAccion abrir(String contrasena) {
        return partida.ejecutarAccion(new AccionAbrirCofre(paquete(CIFRADO), contrasena, servicio));
    }

    @Test
    void contrasenaCorrectaAbreYCuestaUno() {
        assertEquals(ResultadoAccion.OK, abrir("ORBITA-7392"));
        assertEquals(2, partida.getAccionesRestantes());
        assertTrue(partida.estaCofreAbierto());
        Reliquia especial = ana.getInventario().getReliquias().get(0);
        assertEquals("REL-ESPECIAL", especial.getReliquiaId());
        assertEquals(4, especial.getValor());
    }

    @Test
    void minusculasSeConviertenYAbren() {
        assertEquals(ResultadoAccion.OK, abrir("orbita-7392"));
    }

    @Test
    void falloDeAutenticacionCuestaUno() {
        assertEquals(ResultadoAccion.FALLO_AUTENTICACION, abrir("ORBITA7392"));
        assertEquals(2, partida.getAccionesRestantes());
        assertFalse(partida.estaCofreAbierto());
        assertTrue(ana.getInventario().estaVacio());
    }

    @Test
    void falloYLuegoAciertoCuestaDos() {
        abrir("ORBITA7392");
        assertEquals(ResultadoAccion.OK, abrir("ORBITA-7392"));
        assertEquals(1, partida.getAccionesRestantes());
    }

    @Test
    void formatoInvalidoNoCuesta() {
        assertEquals(ResultadoAccion.FORMATO_INVALIDO, abrir("ORBITA 7392"));
        assertEquals(ResultadoAccion.FORMATO_INVALIDO, abrir("ABC"));
        assertEquals(3, partida.getAccionesRestantes());
    }

    @Test
    void paqueteCorruptoNoCuesta() {
        AccionAbrirCofre accion = new AccionAbrirCofre(paquete("%%no-es-base64%%"), "ORBITA-7392", servicio);
        assertEquals(ResultadoAccion.DATOS_CORRUPTOS, partida.ejecutarAccion(accion));
        assertEquals(3, partida.getAccionesRestantes());
        assertFalse(partida.estaCofreAbierto());
    }

    @Test
    void paqueteDeOtraPartidaNoCuesta() {
        PaqueteCofre otro = new PaqueteCofre("OTRA-01", "COFRE-01", "AES/GCM/NoPadding",
                "PBKDF2WithHmacSHA256", 100000, 256, SAL, NONCE, 128, "RVgtUkVELTAxfENPRlJFLTAx", CIFRADO);
        assertEquals(ResultadoAccion.DATOS_CORRUPTOS, partida.ejecutarAccion(new AccionAbrirCofre(otro, "ORBITA-7392", servicio)));
        assertEquals(3, partida.getAccionesRestantes());
    }

    @Test
    void cofreAbiertoNoDaSegundaReliquia() {
        abrir("ORBITA-7392");
        assertEquals(ResultadoAccion.COFRE_YA_ABIERTO, abrir("ORBITA-7392"));
        assertEquals(2, partida.getAccionesRestantes());

        // le toca a Luis, que tambien esta en el cofre
        partida.cambiarTurno();
        assertEquals(ResultadoAccion.COFRE_YA_ABIERTO, abrir("ORBITA-7392"));
        assertEquals(3, partida.getAccionesRestantes());
        assertEquals(1, ana.getInventario().getReliquias().size());
        assertTrue(luis.getInventario().estaVacio());
    }

    @Test
    void validacionesPreviasNoCuestan() {
        Partida sinPistas = new Partida("EX-RED-01", partida.getTablero(), ana, luis);
        AccionAbrirCofre accion = new AccionAbrirCofre(paquete(CIFRADO), "ORBITA-7392", servicio);
        assertEquals(ResultadoAccion.FALTAN_PISTAS, sinPistas.ejecutarAccion(accion));

        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 1));
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));
        assertEquals(ResultadoAccion.INVENTARIO_LLENO, abrir("ORBITA-7392"));

        ana.setPosicion(new Coordenada(2, 2));
        assertEquals(ResultadoAccion.NO_ESTA_EN_COFRE, abrir("ORBITA-7392"));
        assertEquals(3, partida.getAccionesRestantes());
    }
}
