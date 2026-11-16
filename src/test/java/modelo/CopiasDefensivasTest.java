package modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Modificar las listas que devuelven los getters no debe cambiar el modelo. */
class CopiasDefensivasTest {

    @Test
    void modificarCopiasNoCambiaElModelo() {
        Tablero tablero = new Tablero(5, 5);
        Coordenada campamento = new Coordenada(2, 2);
        Participante ana = new Participante(new Jugador("J1", "Ana", "ana"), campamento);
        Participante luis = new Participante(new Jugador("J2", "Luis", "luis"), campamento);
        Partida partida = new Partida("EX-RED-01", tablero, ana, luis);
        partida.agregarPistaDescubierta(new Pista(1, "ORBITA"));
        ana.getInventario().agregarReliquia(new Reliquia("REL-01", 1));
        ana.entregarReliquias();
        ana.getInventario().agregarReliquia(new Reliquia("REL-02", 2));

        // la vista intenta cambiar las listas
        ana.getInventario().getReliquias().clear();
        ana.getEntregadas().clear();
        partida.getPistasDescubiertas().add(new Pista(2, "7392"));

        assertEquals(1, ana.getInventario().getReliquias().size());
        assertEquals(1, ana.getEntregadas().size());
        assertEquals(1, partida.getPistasDescubiertas().size());
    }
}
