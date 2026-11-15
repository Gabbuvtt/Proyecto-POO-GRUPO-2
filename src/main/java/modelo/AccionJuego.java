package modelo;

/** Accion que un participante puede hacer en su turno. */
public interface AccionJuego {
    /** Ejecuta la accion sobre la partida y devuelve OK o el motivo del rechazo. */
    ResultadoAccion ejecutar(Partida partida);
}
