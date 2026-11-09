package modelo;

import java.util.ArrayList;

public class Inventario {
    private ArrayList<Reliquia> reliquias;

    public Inventario() {
        this.reliquias = new ArrayList<>();
    }

    public boolean estaLleno() {
        return reliquias.size() >= ReglasDelJuego.getCapacidadInventario();
    }

    public boolean estaVacio() {
        return reliquias.isEmpty();
    }

    public boolean agregarReliquia(Reliquia nueva) {
        if (estaLleno()) {
            return false; // No cabe porque ya tiene 2
        }
        reliquias.add(nueva);
        return true;
    }

    // Saca todas las reliquias del inventario y las devuelve cuando se entregan en el Campamento
    public ArrayList<Reliquia> entregarTodas() {
        ArrayList<Reliquia> entregadas = new ArrayList<>();
        for (int i = 0; i < reliquias.size(); i++) {
            entregadas.add(reliquias.get(i));
        }
        reliquias.clear();
        return entregadas;
    }

    public ArrayList<Reliquia> getReliquias() {
        return reliquias;
    }
}