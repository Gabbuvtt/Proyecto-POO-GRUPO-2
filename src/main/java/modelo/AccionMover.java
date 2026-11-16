package modelo;

/** Accion de desplazar al participante actual a una casilla vecina ortogonal. */
public class AccionMover implements AccionJuego {
    private Coordenada destino;

    /** Crea la accion de movimiento indicando la coordenada de destino. */
    public AccionMover(Coordenada destino) {
        this.destino = destino;
    }

    /** Valida que el destino sea vecino ortogonal y que alcancen las acciones antes de mover al participante. */
    @Override
    public ResultadoAccion ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO) {
            return ResultadoAccion.PARTIDA_FINALIZADA;
        }

        if (!partida.getTablero().estaDentroDelTablero(destino)) {
            return ResultadoAccion.FUERA_DEL_MAPA;
        }

        Participante actual = partida.getTurnoActual();
        Coordenada origen = actual.getPosicion();

        if (!origen.esVecinaOrtogonal(destino)) {
            return ResultadoAccion.NO_ES_VECINO;
        }

        Casilla casillaDestino = partida.getTablero().obtenerCasilla(destino);
        int costo = casillaDestino.getTipo().getCosto();

        if (partida.getAccionesRestantes() < costo) {
            return ResultadoAccion.SIN_ACCIONES;
        }

        actual.setPosicion(destino);
        partida.consumirAcciones(costo);
        return ResultadoAccion.OK;
    }
}