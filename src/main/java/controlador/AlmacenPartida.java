
package controlador;
import modelo.Partida;
import servicio.PaqueteCofre;
public interface AlmacenPartida {

    /** Guarda la partida. Devuelve true solo si la API confirmo el guardado. */
    boolean guardar(Partida partida);

    /** Pide el paquete cifrado del cofre  */
    PaqueteCofre obtenerPaqueteCofre(String partidaId);

    /** Pide la reliquia y la pista del templo en la coordenada dada. */
    ContenidoTemplo obtenerContenidoTemplo(String partidaId, String coordenada);
}
