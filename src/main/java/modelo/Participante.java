package modelo;

import java.util.ArrayList;

public class Participante {
    private Jugador jugador;
    private Coordenada posicion;
    private int puntos;
    private Inventario inventario;
    private ArrayList<Reliquia> entregadas;
    private long segundosConsumidos;

    public Participante(Jugador jugador, Coordenada posicionInicial) {
        this.jugador = jugador;
        this.posicion = posicionInicial;
        this.puntos = 0;
        this.inventario = new Inventario();
        this.entregadas = new ArrayList<>();
        this.segundosConsumidos = 0;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public String getJugadorId() {
        return jugador.getJugadorId();
    }

    public Coordenada getPosicion() {
        return posicion;
    }

    public void setPosicion(Coordenada nuevaPosicion) {
        this.posicion = nuevaPosicion;
    }

    public int getPuntos() {
        return puntos;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public ArrayList<Reliquia> getEntregadas() {
        return entregadas;
    }

    public long getSegundosConsumidos() {
        return segundosConsumidos;
    }

    public void sumarSegundos(long segundos) {
        if (segundos > 0) {
            this.segundosConsumidos = this.segundosConsumidos + segundos;
        }
    }

    // Saca todas las reliquias del inventario, las pasa a entregadas y suma sus puntos
    public int entregarReliquias() {
        ArrayList<Reliquia> sacadas = inventario.entregarTodas();
        int puntosGanados = 0;

        for (int i = 0; i < sacadas.size(); i++) {
            Reliquia r = sacadas.get(i);
            entregadas.add(r);
            puntosGanados = puntosGanados + r.getValor();
        }

        this.puntos = this.puntos + puntosGanados;
        return puntosGanados;
    }
}