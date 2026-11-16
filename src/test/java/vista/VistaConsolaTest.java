package vista;

import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VistaConsolaTest {

    @Test
    void leerEnteroIgnoraLetrasVaciosYNumerosFueraDeRango() {
        VistaConsola vista = new VistaConsola(new Scanner("abc\n9\n\n2.5\n3\n"));
        assertEquals(3, vista.leerEntero("n: ", 0, 5));
    }

    @Test
    void leerEnteroAceptaEspaciosAlrededorDelNumero() {
        VistaConsola vista = new VistaConsola(new Scanner("  4  \n"));
        assertEquals(4, vista.leerEntero("n: ", 1, 7));
    }

    @Test
    void menuPrincipalAceptaDel0Al5() {
        assertEquals(0, new VistaConsola(new Scanner("0\n")).pedirOpcionMenuPrincipal());
        assertEquals(5, new VistaConsola(new Scanner("5\n")).pedirOpcionMenuPrincipal());
    }

    @Test
    void sinMasEntradaLanzaExcepcionEnVezDeQuedarseEnBucle() {
        VistaConsola vista = new VistaConsola(new Scanner("abc\n"));
        assertThrows(IllegalStateException.class, () -> vista.leerEntero("n: ", 1, 7));
    }
}