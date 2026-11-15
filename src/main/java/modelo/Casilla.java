package modelo;

public class Casilla {
    private Coordenada coordenada;
    private TipoDeTerreno tipo;

    public Casilla(Coordenada coordenada, TipoDeTerreno tipo) {
        this.coordenada = coordenada;
        this.tipo = tipo;
    }

    public Coordenada getCoordenada() {
        return coordenada;
    }

    public TipoDeTerreno getTipo() {
        return tipo;
    }

    public void setTipo(TipoDeTerreno tipo) {
        this.tipo = tipo;
    }
}
