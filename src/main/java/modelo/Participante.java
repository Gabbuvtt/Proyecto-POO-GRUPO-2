package modelo;

import java.util.ArrayList;

/** Representa el estado de un jugador dentro de una partida (posicion, puntos, inventario y tiempo). */
public class Participante {
    private Jugador jugador;
    private Coordenada posicion;
    private int puntos;
    private Inventario inventario;
    private ArrayList<Reliquia> entregadas;
    private long segundosConsumidos;

    /** Crea un participante asociado a un jugador y ubicado en la posicion inicial dada. */
    public Participante(Jugador jugador, Coordenada posicionInicial) {
        this.jugador = jugador;
        this.posicion = posicionInicial;
        this.puntos = 0;
        this.inventario = new Inventario();
        this.entregadas = new ArrayList<>();
        this.segundosConsumidos = 0;
    }

    /** Devuelve el objeto Jugador asociado a este participante. */
    public Jugador getJugador() {
        return jugador;
    }

    /** Devuelve el identificador unico del jugador. */
    public String getJugadorId() {
        return jugador.getJugadorId();
    }

    /** Devuelve la coordenada actual del participante en el tablero. */
    public Coordenada getPosicion() {
        return posicion;
    }

    /** Actualiza la coordenada actual del participante en el tablero. */
    public void setPosicion(Coordenada nuevaPosicion) {
        this.posicion = nuevaPosicion;
    }

    /** Devuelve el puntaje acumulado por reliquias entregadas. */
    public int getPuntos() {
        return puntos;
    }

    /** Devuelve el inventario actual del participante. */
    public Inventario getInventario() {
        return inventario;
    }

    /** Devuelve la lista de todas las reliquias entregadas en el campamento. */
    public ArrayList<Reliquia> getEntregadas() {
        return entregadas;
    }

    /** Devuelve el total de segundos consumidos por el participante en sus turnos. */
    public long getSegundosConsumidos() {
        return segundosConsumidos;
    }

    /** Acumula segundos al tiempo total consumido por el participante. */
    public void sumarSegundos(long segundos) {
        if (segundos > 0) {
            this.segundosConsumidos = this.segundosConsumidos + segundos;
        }
    }

    /** Pasa todas las reliquias del inventario a la lista de entregadas y suma su valor al puntaje. */
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