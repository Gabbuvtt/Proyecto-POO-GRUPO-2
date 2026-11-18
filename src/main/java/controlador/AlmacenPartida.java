
package controlador;
import modelo.Jugador;
import modelo.Partida;
import servicio.PaqueteCofre;

import java.util.ArrayList;

/** Operaciones que el controlador necesita de la API. */
public interface AlmacenPartida {

    /** Indica si la API responde con la version de contrato esperada. */
    boolean apiDisponible();

    /** Registra un jugador y devuelve su jugadorId. */
    String registrarJugador(String nombre, String alias);

    /** Busca un jugador por alias. Devuelve null si no existe. */
    Jugador buscarJugadorPorAlias(String alias);

    /** Crea una partida para dos jugadores y devuelve su partidaId. */
    String crearPartida(String jugador1, String jugador2);

    /** Lista las partidas de un jugador. */
    ArrayList<ResumenPartida> listarPartidas(String jugadorId);

    /** Trae una partida guardada con todo su estado. */
    Partida cargarPartida(String partidaId);

    /** Consulta las estadisticas de un jugador. */
    EstadisticasJugador obtenerEstadisticas(String jugadorId);

    /**
     * Guarda la partida. Devuelve true solo si la API confirmo el guardado.
     * Lanza ConflictoVersionException si la API tiene una revision mas nueva.
     */
    boolean guardar(Partida partida);

    /** Pide el paquete cifrado del cofre  */
    PaqueteCofre obtenerPaqueteCofre(String partidaId);

    /** Pide la reliquia y la pista del templo en la coordenada dada. */
    ContenidoTemplo obtenerContenidoTemplo(String partidaId, String coordenada);
}
