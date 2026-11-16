package vista;

import modelo.Casilla;
import modelo.Coordenada;
import modelo.Participante;
import modelo.Tablero;
/** Muestra por consola las pantallas del juego. Solo imprime: no decide nada de las reglas. */
public class VistaConsola {

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