package modelo;

public class Jugador {
    private String jugadorId;
    private String nombre;
    private String alias;

    public Jugador(String jugadorId, String nombre, String alias) {
        this.jugadorId = jugadorId;
        this.nombre = nombre;
        this.alias = alias;
    }

    public String getJugadorId() {
        return jugadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAlias() {
        return alias;
    }
}