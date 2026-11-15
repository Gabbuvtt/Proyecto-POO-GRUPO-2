package modelo;

public class AccionExplorar implements AccionJuego {

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
        Casilla casillaActual = partida.getTablero().obtenerCasilla(pos);
        TipoDeTerreno terreno = casillaActual.getTipo();

        // Caso 1 Explorar un Templo (T)
        if (terreno == TipoDeTerreno.TEMPLO) {
            if (partida.yaFueExplorado(pos)) {
                return ResultadoAccion.TEMPLO_YA_EXPLORADO;
            }

            Reliquia reliquia = partida.obtenerReliquiaOculta(pos);
            if (reliquia != null) {
                // agregarreliquia ya verifica internamente con estaLleno() si cabe o no
                boolean seAgrego = actual.getInventario().agregarReliquia(reliquia);
                if (!seAgrego) {
                    return ResultadoAccion.INVENTARIO_LLENO;
                }
            }

            Pista pista = partida.obtenerPistaOculta(pos);
            if (pista != null) {
                partida.agregarPistaDescubierta(pista);
            }

            partida.marcarComoExplorado(pos);
            partida.consumirAcciones(1);
            return ResultadoAccion.OK;
        }

        return ResultadoAccion.NO_ES_TEMPLO;
    }
}