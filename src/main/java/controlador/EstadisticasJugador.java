package controlador;

/** Estadisticas de un jugador calculadas por la API. */
public class EstadisticasJugador {

    private final int partidasFinalizadas;
    private final int victorias;
    private final int puntosAcumulados;
    private final int reliquiasEntregadas;

    /** Crea las estadisticas con los valores recibidos. */
    public EstadisticasJugador(int partidasFinalizadas, int victorias,
                               int puntosAcumulados, int reliquiasEntregadas) {
        this.partidasFinalizadas = partidasFinalizadas;
        this.victorias = victorias;
        this.puntosAcumulados = puntosAcumulados;
        this.reliquiasEntregadas = reliquiasEntregadas;
    }

    /** Devuelve las partidas finalizadas. */
    public int getPartidasFinalizadas() {
        return partidasFinalizadas;
    }

    /** Devuelve las victorias, contando los empates. */
    public int getVictorias() {
        return victorias;
    }

    /** Devuelve los puntos sumados en todas las partidas finalizadas. */
    public int getPuntosAcumulados() {
        return puntosAcumulados;
    }

    /** Devuelve las reliquias entregadas en todas las partidas finalizadas. */
    public int getReliquiasEntregadas() {
        return reliquiasEntregadas;
    }
}
