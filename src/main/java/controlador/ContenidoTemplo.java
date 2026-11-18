package controlador;

import modelo.Pista;
import modelo.Reliquia;

/** Reliquia y pista de un templo segun la API. Cualquiera puede ser null. */
public class ContenidoTemplo {

    private final Reliquia reliquia;
    private final Pista pista;

    /** Crea el contenido con la reliquia y la pista recibidas. */
    public ContenidoTemplo(Reliquia reliquia, Pista pista) {
        this.reliquia = reliquia;
        this.pista = pista;
    }

    /** Devuelve la reliquia del templo, o null si no tiene. */
    public Reliquia getReliquia() {
        return reliquia;
    }

    /** Devuelve la pista del templo, o null si no tiene. */
    public Pista getPista() {
        return pista;
    }
}
