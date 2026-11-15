package modelo;

public class AccionEntregar implements AccionJuego {

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