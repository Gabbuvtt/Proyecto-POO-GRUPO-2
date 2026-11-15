package modelo;

public class Pista {
    private int indice;
    private String fragmento;

    public Pista(int indice, String fragmento) {
        this.indice = indice;
        this.fragmento = fragmento;
    }

    public int getIndice() {
        return indice;
    }

    public String getFragmento() {
        return fragmento;
    }
}