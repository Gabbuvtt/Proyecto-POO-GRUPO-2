package modelo;

public class Tablero {
    private int filas;
    private int columnas;
    private Casilla[][] matriz;

    public Tablero(int filas, int columnas) {
        this.filas = filas;
        this.columnas = columnas;
        this.matriz = new Casilla[filas][columnas];

        // Rellenamos todo el tablero inicialmente con Senderos ('S')
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                Coordenada coord = new Coordenada(f, c);
                matriz[f][c] = new Casilla(coord, TipoDeTerreno.SENDERO);
            }
        }
    }

    public int getFilas() {
        return filas;
    }

    public int getColumnas() {
        return columnas;
    }

    // Revisa que la coordenada no se salga del mapa (por ejemplo, que no sea -1 ni mayor al límite)
    public boolean estaDentroDelTablero(Coordenada coord) {
        if (coord == null) {
            return false;
        }
        if (coord.getFila() >= 0 && coord.getFila() < filas &&
                coord.getColumna() >= 0 && coord.getColumna() < columnas) {
            return true;
        } else {
            return false;
        }
    }

    // Cambia el terreno de una casilla (ej. poner el Campamento en C3 o un Pantano en D1)
    public void colocarTerreno(Coordenada coord, TipoDeTerreno tipo) {
        if (estaDentroDelTablero(coord)) {
            matriz[coord.getFila()][coord.getColumna()].setTipo(tipo);
        }
    }

    // Devuelve la casilla que está en esa coordenada
    public Casilla obtenerCasilla(Coordenada coord) {
        if (estaDentroDelTablero(coord)) {
            return matriz[coord.getFila()][coord.getColumna()];
        }
        return null;
    }
}