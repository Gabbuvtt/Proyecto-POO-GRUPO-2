package modelo;

/** Representa los datos basicos de un jugador registrado en el sistema. */
public class Jugador {
    private String jugadorId;
    private String nombre;
    private String alias;

    /** Crea un jugador con su identificador unico, nombre y alias. */
    public Jugador(String jugadorId, String nombre, String alias) {
        this.jugadorId = jugadorId;
        this.nombre = nombre;
        this.alias = alias;
    }

    /** Devuelve el identificador unico del jugador. */
    public String getJugadorId() {
        return jugadorId;
    }

    /** Devuelve el nombre del jugador. */
    public String getNombre() {
        return nombre;
    }

    /** Devuelve el alias del jugador. */
    public String getAlias() {
        return alias;
    }
}