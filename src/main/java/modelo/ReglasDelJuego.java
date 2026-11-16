package modelo;

/** Constantes globales que definen las reglas parametrizadas del juego. */
public class ReglasDelJuego {
    public static final int ACCIONES_POR_TURNO = 3;
    public static final int MAX_RONDAS = 8;
    public static final int CAPACIDAD_INVENTARIO = 2;
    public static final int VALOR_RELIQUIA_ESPECIAL = 4;
    public static final int CANTIDAD_PISTAS = 2;

    /** Devuelve la cantidad de acciones permitidas por turno (3). */
    public static int getAccionesPorTurno() {
        return ACCIONES_POR_TURNO;
    }

    /** Devuelve el numero maximo de rondas de una partida (8). */
    public static int getMaxRondas() {
        return MAX_RONDAS;
    }

    /** Devuelve la cantidad maxima de reliquias que caben en el inventario (2). */
    public static int getCapacidadInventario() {
        return CAPACIDAD_INVENTARIO;
    }

    /** Devuelve el valor en puntos de la reliquia especial del cofre (4). */
    public static int getValorReliquiaEspecial() {
        return VALOR_RELIQUIA_ESPECIAL;
    }

    /** Devuelve la cantidad de pistas necesarias para abrir el cofre (2). */
    public static int getCantidadPistas() {
        return CANTIDAD_PISTAS;
    }
}