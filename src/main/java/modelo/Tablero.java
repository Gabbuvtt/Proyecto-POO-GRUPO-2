package modelo;

/** Representa la matriz de casillas que conforman el mapa de la partida. */
public class Tablero {
    private int filas;
    private int columnas;
    private Casilla[][] matriz;

    /** Crea un tablero de las dimensiones dadas inicializando todas las casillas como sendero. */
    public Tablero(int filas, int columnas) {
        this.filas = filas;
        this.columnas = columnas;
        this.matriz = new Casilla[filas][columnas];

        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                Coordenada coord = new Coordenada(f, c);
                matriz[f][c] = new Casilla(coord, TipoDeTerreno.SENDERO);
            }
        }
    }

    /** Devuelve la cantidad de filas del tablero. */
    public int getFilas() {
        return filas;
    }

    /** Devuelve la cantidad de columnas del tablero. */
    public int getColumnas() {
        return columnas;
    }

    /** Indica si una coordenada se encuentra dentro de los limites del tablero. */
    public boolean estaDentroDelTablero(Coordenada coord) {
        if (coord == null) {
            return false;
        }
        return coord.getFila() >= 0 && coord.getFila() < filas &&
                coord.getColumna() >= 0 && coord.getColumna() < columnas;
    }

    /** Asigna un tipo de terreno a la casilla ubicada en la coordenada indicada. */
    public void colocarTerreno(Coordenada coord, TipoDeTerreno tipo) {
        if (estaDentroDelTablero(coord)) {
            matriz[coord.getFila()][coord.getColumna()].setTipo(tipo);
        }
    }

    /** Devuelve la casilla correspondiente a la coordenada dada, o null si esta fuera del tablero. */
    public Casilla obtenerCasilla(Coordenada coord) {
        if (estaDentroDelTablero(coord)) {
            return matriz[coord.getFila()][coord.getColumna()];
        }
        return null;
    }
}