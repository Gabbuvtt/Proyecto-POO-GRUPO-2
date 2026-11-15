package modelo;

public class AccionExplorar implements AccionJuego {

    @Override
    public boolean ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO || partida.getAccionesRestantes() < 1) {
            return false;
        }

        Participante actual = partida.getTurnoActual();
        Coordenada pos = actual.getPosicion();
        Casilla casillaActual = partida.getTablero().obtenerCasilla(pos);
        TipoDeTerreno terreno = casillaActual.getTipo();

        // Caso 1 Explorar un Templo (T)
        if (terreno == TipoDeTerreno.TEMPLO) {
            if (partida.yaFueExplorado(pos)) {
                return false;
            }

            Reliquia reliquia = partida.obtenerReliquiaOculta(pos);
            if (reliquia != null) {
                // agregarreliquia ya verifica internamente con estaLleno() si cabe o no
                boolean seAgrego = actual.getInventario().agregarReliquia(reliquia);
                if (!seAgrego) {
                    return false;
                }
            }

            Pista pista = partida.obtenerPistaOculta(pos);
            if (pista != null) {
                partida.agregarPistaDescubierta(pista);
            }

            partida.marcarComoExplorado(pos);
            partida.consumirAcciones(1);
            return true;
        }

        // Caso 2 Abrir el Cofre (X)
        if (terreno == TipoDeTerreno.COFRE) {
            if (!partida.tienenTodasLasPistas()) {
                return false;
            }

            actual.sumarPuntos(ReglasDelJuego.getValorReliquiaEspecial());
            partida.finalizarPartidaPorCofre();
            return true;
        }

        return false;
    }
}