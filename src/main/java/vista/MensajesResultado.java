package vista;

import modelo.ResultadoAccion;

/** Traduce cada ResultadoAccion del modelo a una frase en español para mostrarle al jugador. */
public class MensajesResultado {

    /** Devuelve el texto que se le muestra al jugador para el resultado dado. */
    public String describir(ResultadoAccion resultado) {
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado no puede ser nulo");
        }
        return switch (resultado) {
            case OK -> "Acción realizada.";
            case PARTIDA_FINALIZADA -> "La partida ya terminó. Solo se puede consultar el resultado.";
            case NO_ES_VECINO -> "Movimiento inválido: el destino no es vecino ortogonal de tu posición actual.";
            case FUERA_DEL_MAPA -> "Esa coordenada está fuera del mapa.";
            case SIN_ACCIONES -> "No te alcanzan los puntos de acción para eso.";
            case NO_ES_TEMPLO -> "Aquí no hay un templo para explorar.";
            case TEMPLO_YA_EXPLORADO -> "Explorado y vacío.";
            case INVENTARIO_LLENO -> "Inventario lleno. Regrese al campamento para entregar antes de explorar o abrir el cofre.";
            case NO_ESTA_EN_CAMPAMENTO -> "Debes estar en el campamento para entregar.";
            case INVENTARIO_VACIO -> "No llevas reliquias para entregar.";
            case NO_ESTA_EN_COFRE -> "Debes estar en el cofre (X) para intentar abrirlo.";
            case FALTAN_PISTAS -> "Todavía no se conocen las dos pistas del cofre.";
            case COFRE_YA_ABIERTO -> "El cofre ya está abierto.";
            case FORMATO_INVALIDO -> "Formato de contraseña inválido: usa de 4 a 20 letras o números, con un guion opcional.";
            case FALLO_AUTENTICACION -> "No se pudo autenticar el archivo.";
            case DATOS_CORRUPTOS -> "Los datos del cofre están corruptos. No se cobró ninguna acción.";
        };
    }
}