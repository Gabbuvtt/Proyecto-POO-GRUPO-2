package modelo;

public class AccionMover implements AccionJuego {
    private Coordenada destino;

    public AccionMover(Coordenada destino) {
        this.destino = destino;
    }

    @Override
    public boolean ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO) {
            return false;
        }

        if (!partida.getTablero().estaDentroDelTablero(destino)) {
            return false;
        }

        Participante actual = partida.getTurnoActual();
        Coordenada origen = actual.getPosicion();

        if (!origen.esVecinaOrtogonal(destino)) {
            return false;
        }

        Casilla casillaDestino = partida.getTablero().obtenerCasilla(destino);
        int costo = casillaDestino.getTipo().getCosto();

        if (partida.getAccionesRestantes() < costo) {
            return false;
        }

        actual.setPosicion(destino);
        partida.consumirAcciones(costo);
        return true;
    }
}