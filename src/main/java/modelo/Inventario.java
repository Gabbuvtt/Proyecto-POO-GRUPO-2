package modelo;

import java.util.ArrayList;

/** Almacena temporalmente las reliquias recolectadas por un participante antes de entregarlas. */
public class Inventario {
    private ArrayList<Reliquia> reliquias;

    /** Crea un inventario vacio. */
    public Inventario() {
        this.reliquias = new ArrayList<>();
    }

    /** Indica si el inventario alcanzo el limite maximo de reliquias permitidas (2). */
    public boolean estaLleno() {
        return reliquias.size() >= ReglasDelJuego.CAPACIDAD_INVENTARIO;
    }

    /** Indica si el inventario no contiene ninguna reliquia. */
    public boolean estaVacio() {
        return reliquias.isEmpty();
    }

    /** Agrega una reliquia al inventario si no esta lleno y devuelve true si tuvo exito. */
    public boolean agregarReliquia(Reliquia nueva) {
        if (estaLleno()) {
            return false;
        }
        reliquias.add(nueva);
        return true;
    }

    /** Retira todas las reliquias del inventario y devuelve la lista con las reliquias extraidas. */
    public ArrayList<Reliquia> entregarTodas() {
        ArrayList<Reliquia> entregadas = new ArrayList<>();
        for (int i = 0; i < reliquias.size(); i++) {
            entregadas.add(reliquias.get(i));
        }
        reliquias.clear();
        return entregadas;
    }

    /** Devuelve una copia de las reliquias que estan actualmente en el inventario. */
    public ArrayList<Reliquia> getReliquias() {
        return new ArrayList<>(reliquias);
    }
}