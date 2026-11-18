package controlador;

/** Datos basicos de una partida para mostrarla en la lista de carga. */
public class ResumenPartida {

    private final String partidaId;
    private final String jugadores;
    private final String estado;
    private final int ronda;

    /** Crea el resumen. jugadores es el texto con los alias, ej. "ana_ucab, luis_ucab". */
    public ResumenPartida(String partidaId, String jugadores, String estado, int ronda) {
        this.partidaId = partidaId;
        this.jugadores = jugadores;
        this.estado = estado;
        this.ronda = ronda;
    }

    /** Devuelve el identificador de la partida. */
    public String getPartidaId() {
        return partidaId;
    }

    /** Devuelve los alias de los jugadores separados por coma. */
    public String getJugadores() {
        return jugadores;
    }

    /** Devuelve EN_CURSO o FINALIZADA. */
    public String getEstado() {
        return estado;
    }

    /** Devuelve la ronda guardada. */
    public int getRonda() {
        return ronda;
    }
}
