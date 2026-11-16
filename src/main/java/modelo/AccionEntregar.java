package modelo;

/** Accion de entregar todas las reliquias del inventario en el campamento (C). */
public class AccionEntregar implements AccionJuego {

    /** Ejecuta la entrega de reliquias si el participante esta en el campamento y su inventario no esta vacio. */
    @Override
    public ResultadoAccion ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO) {
            return ResultadoAccion.PARTIDA_FINALIZADA;
        }

        if (partida.getAccionesRestantes() < 1) {
            return ResultadoAccion.SIN_ACCIONES;
        }

        Participante actual = partida.getTurnoActual();
        Casilla casillaActual = partida.getTablero().obtenerCasilla(actual.getPosicion());

        if (casillaActual.getTipo() != TipoDeTerreno.CAMPAMENTO) {
            return ResultadoAccion.NO_ESTA_EN_CAMPAMENTO;
        }

        if (actual.getInventario().estaVacio()) {
            return ResultadoAccion.INVENTARIO_VACIO;
        }

        actual.entregarReliquias();
        partida.consumirAcciones(1);
        return ResultadoAccion.OK;
    }
}