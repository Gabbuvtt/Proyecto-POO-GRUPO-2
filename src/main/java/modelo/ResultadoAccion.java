package modelo;

/** Resultado de ejecutar una accion del juego: OK o el motivo por el que no se hizo. */
public enum ResultadoAccion {
    OK,
    PARTIDA_FINALIZADA,
    NO_ES_VECINO,
    FUERA_DEL_MAPA,
    SIN_ACCIONES,
    NO_ES_TEMPLO,
    TEMPLO_YA_EXPLORADO,
    INVENTARIO_LLENO,
    NO_ESTA_EN_CAMPAMENTO,
    INVENTARIO_VACIO,
    NO_ESTA_EN_COFRE,
    FALTAN_PISTAS,
    COFRE_YA_ABIERTO,
    FORMATO_INVALIDO,
    FALLO_AUTENTICACION,
    DATOS_CORRUPTOS
}
