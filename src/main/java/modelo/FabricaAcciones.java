
package modelo;

import servicio.PaqueteCofre;
import servicio.ServicioCriptografico;

/** Fabrica encargada de crear las acciones del juego segun la opcion del menu (Patron Factory). */
public class FabricaAcciones {

    /** Crea una accion de movimiento hacia la coordenada destino indicada. */
    public static AccionJuego crearAccionMover(Coordenada destino) {
        return new AccionMover(destino);
    }

    /** Crea una accion para explorar un templo en la casilla actual. */
    public static AccionJuego crearAccionExplorar() {
        return new AccionExplorar();
    }

    /** Crea una accion para entregar reliquias en el campamento. */
    public static AccionJuego crearAccionEntregar() {
        return new AccionEntregar();
    }

    /** Crea una accion para intentar abrir el cofre con el paquete cifrado y la contrasena. */
    public static AccionJuego crearAccionAbrirCofre(PaqueteCofre paquete, String contrasena, ServicioCriptografico servicio) {
        return new AccionAbrirCofre(paquete, contrasena, servicio);
    }
}
