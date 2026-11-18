
package modelo;

/**
 * Accion de explorar un templo para recolectar reliquias y descubrir pistas
 */
public class AccionExplorar implements AccionJuego {

    /** Recoge la reliquia y la pista del templo actual. Cuesta 1 accion. */
    @Override
    public ResultadoAccion ejecutar(Partida partida) {

        if (partida.getEstado() != EstadoDePartida.EN_CURSO) {
            return ResultadoAccion.PARTIDA_FINALIZADA;
        }

        if (partida.getAccionesRestantes() < 1) {
            return ResultadoAccion.SIN_ACCIONES;
        }

        Participante actual = partida.getTurnoActual();
        Coordenada pos = actual.getPosicion();

        Casilla casillaActual =
                partida.getTablero().obtenerCasilla(pos);

        if (casillaActual.getTipo() != TipoDeTerreno.TEMPLO) {
            return ResultadoAccion.NO_ES_TEMPLO;
        }

        if (partida.yaFueExplorado(pos)) {
            return ResultadoAccion.TEMPLO_YA_EXPLORADO;
        }

        // el controlador ya dejo en la partida lo que trajo de la API
        Reliquia reliquia = partida.obtenerReliquiaOculta(pos);
        Pista pista = partida.obtenerPistaOculta(pos);

        if (reliquia != null) {
            boolean seAgrego =
                    actual.getInventario().agregarReliquia(reliquia);

            if (!seAgrego) {
                return ResultadoAccion.INVENTARIO_LLENO;
            }
        }

        if (pista != null) {
            partida.agregarPistaDescubierta(pista);
        }

        partida.marcarComoExplorado(pos);
        partida.consumirAcciones(1);

        return ResultadoAccion.OK;
    }
}
