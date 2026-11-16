package modelo;

/** Representa una celda individual del tablero con su coordenada y tipo de terreno. */
public class Casilla {
    private Coordenada coordenada;
    private TipoDeTerreno tipo;

    /** Crea una casilla en la coordenada indicada con un tipo de terreno inicial. */
    public Casilla(Coordenada coordenada, TipoDeTerreno tipo) {
        this.coordenada = coordenada;
        this.tipo = tipo;
    }

    /** Devuelve la coordenada donde se ubica la casilla. */
    public Coordenada getCoordenada() {
        return coordenada;
    }

    /** Devuelve el tipo de terreno actual de la casilla. */
    public TipoDeTerreno getTipo() {
        return tipo;
    }

    /** Cambia el tipo de terreno asignado a la casilla. */
    public void setTipo(TipoDeTerreno tipo) {
        this.tipo = tipo;
    }
}