package modelo;

public class AccionEntregar implements AccionJuego {

    @Override
    public boolean ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO || partida.getAccionesRestantes() < 1) {
            return false;
        }

        Participante actual = partida.getTurnoActual();
        Casilla casillaActual = partida.getTablero().obtenerCasilla(actual.getPosicion());

        if (casillaActual.getTipo() != TipoDeTerreno.CAMPAMENTO) {
            return false;
        }

        if (actual.getInventario().estaVacio()) {
            return false;
        }

        actual.entregarReliquias();
        partida.consumirAcciones(1);
        return true;
    }
}