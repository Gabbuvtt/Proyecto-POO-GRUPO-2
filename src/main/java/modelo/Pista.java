package modelo;

/** Representa un fragmento de pista oculto en un templo para formar la contrasena del cofre. */
public class Pista {
    private int indice;
    private String fragmento;

    /** Crea una pista con su indice de orden y el fragmento de texto de la contrasena. */
    public Pista(int indice, String fragmento) {
        this.indice = indice;
        this.fragmento = fragmento;
    }

    /** Devuelve el numero de orden de la pista (1 o 2). */
    public int getIndice() {
        return indice;
    }

    /** Devuelve el texto del fragmento de la pista. */
    public String getFragmento() {
        return fragmento;
    }
}