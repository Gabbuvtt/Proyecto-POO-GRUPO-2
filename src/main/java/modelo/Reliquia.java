package modelo;

/** Representa una reliquia con su identificador y valor en puntos. */
public class Reliquia {
    private String reliquiaId;
    private int valor;

    /** Crea una reliquia indicando su identificador unico y su valor en puntos. */
    public Reliquia(String reliquiaId, int valor) {
        this.reliquiaId = reliquiaId;
        this.valor = valor;
    }

    /** Devuelve el identificador de la reliquia. */
    public String getReliquiaId() {
        return reliquiaId;
    }

    /** Devuelve el valor en puntos de la reliquia. */
    public int getValor() {
        return valor;
    }
}