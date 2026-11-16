package vista;

import modelo.Casilla;
import modelo.Coordenada;
import modelo.Participante;
import modelo.Tablero;

import java.util.Scanner;

/** Muestra por consola las pantallas del juego. Solo imprime y lee: no decide nada de las reglas. */
public class VistaConsola {

    private final Scanner entrada;

    /** Crea la vista leyendo del teclado. */
    public VistaConsola() {
        this(new Scanner(System.in));
    }

    /** Crea la vista leyendo de un Scanner dado (sirve para probarla sin teclado). */
    public VistaConsola(Scanner entrada) {
        this.entrada = entrada;
    }

    /** Imprime un mensaje normal seguido de un salto de linea. */
    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    }

    /** Imprime un mensaje de error para que el usuario sepa que algo no se pudo hacer. */
    public void mostrarError(String mensaje) {
        System.out.println("Error: " + mensaje);
    }

    /** Muestra un texto y devuelve la linea que escribio el usuario, sin espacios en los extremos. */
    public String leerTexto(String mensaje) {
        System.out.print(mensaje);
        if (!entrada.hasNextLine()) {
            throw new IllegalStateException("No hay mas datos de entrada.");
        }
        return entrada.nextLine().trim();
    }

    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                int numero = Integer.parseInt(texto);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // no era un numero: se muestra el aviso de abajo
            }
            mostrarError("escribe un numero entre " + minimo + " y " + maximo + ".");
        }
    }

    /** Muestra el menu principal y devuelve la opcion elegida (0 a 5). */
    public int pedirOpcionMenuPrincipal() {
        System.out.println();
        System.out.println("EXPEDICIÓN - LA ISLA DE LAS RELIQUIAS");
        System.out.println("1. Registrar jugador");
        System.out.println("2. Nueva partida");
        System.out.println("3. Cargar partida");
        System.out.println("4. Consultar estadísticas");
        System.out.println("5. Reglas y ayuda");
        System.out.println("0. Salir");
        return leerEntero("Opción: ", 0, 5);
    }

    public void mostrarTablero(Tablero tablero, Participante p1, Participante p2) {
        StringBuilder texto = new StringBuilder();

        // Encabezado: A B C D E
        texto.append("     ");
        for (int c = 0; c < tablero.getColumnas(); c++) {
            texto.append((char) ('A' + c)).append("    ");
        }
        texto.append("\n");

        // Una linea por fila del tablero
        for (int f = 0; f < tablero.getFilas(); f++) {
            texto.append(" ").append(f + 1).append("   ");
            for (int c = 0; c < tablero.getColumnas(); c++) {
                Coordenada coord = new Coordenada(f, c);
                Casilla casilla = tablero.obtenerCasilla(coord);

                String celda = String.valueOf(casilla.getTipo().getSimbolo());
                if (p1.getPosicion().esIgual(coord)) {
                    celda = celda + "1";
                }
                if (p2.getPosicion().esIgual(coord)) {
                    celda = celda + "2";
                }
                texto.append(String.format("%-5s", celda));
            }
            texto.append("\n");
        }

        System.out.print(texto);
    }
}