package modelo;

public class Reliquia {
    private String reliquiaId;
    private int valor;

    public Reliquia(String reliquiaId, int valor) {
        this.reliquiaId = reliquiaId;
        this.valor = valor;
    }

    public String getReliquiaId() {
        return reliquiaId;
    }

    public int getValor() {
        return valor;
    }
}