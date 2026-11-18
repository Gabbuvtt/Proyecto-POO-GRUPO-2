
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

    /**
     * Crea y devuelve la accion correspondiente segun la opcion seleccionada en el menu.
     * Opciones: 1 = Mover, 2 = Explorar, 3 = Entregar, 4 = Abrir Cofre.
     */
    public static AccionJuego crearAccion(int opcion, Coordenada destino, PaqueteCofre paquete, String contrasena, ServicioCriptografico servicio) {
        switch (opcion) {
            case 1:
                return new AccionMover(destino);
            case 2:
                return new AccionExplorar();
            case 3:
                return new AccionEntregar();
            case 4:
                return new AccionAbrirCofre(paquete, contrasena, servicio);
            default:
                throw new IllegalArgumentException("Opcion de accion no valida: " + opcion);
        }
    }

    /** Sobrecarga para crear acciones de Mover (1), Explorar (2) o Entregar (3) desde el menu. */
    public static AccionJuego crearAccion(int opcion, Coordenada destino) {
        return crearAccion(opcion, destino, null, null, null);
    }

    /**
     * Crea una accion de exploracion utilizando
     * la reliquia y la pista recibidas de la API.
     */
    public static AccionJuego crearAccionExplorar(
            Reliquia reliquia,
            Pista pista) {

        return new AccionExplorar(reliquia, pista);
    }

}