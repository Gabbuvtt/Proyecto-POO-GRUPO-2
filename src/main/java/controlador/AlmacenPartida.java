
package controlador;
import modelo.Partida;
import servicio.PaqueteCofre;
public interface AlmacenPartida {

    /** Guarda la partida. Devuelve true solo si la API confirmo el guardado. */
    boolean guardar(Partida partida);

    /** Pide el paquete cifrado del cofre  */
    PaqueteCofre obtenerPaqueteCofre(String partidaId);
}
