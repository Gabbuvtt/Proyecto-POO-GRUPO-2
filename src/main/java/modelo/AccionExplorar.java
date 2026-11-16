package modelo;

/** Accion de explorar un templo (T) para recolectar reliquias y descubrir pistas. */
public class AccionExplorar implements AccionJuego {

    /** Explora el templo en la posicion actual si no ha sido explorado y hay espacio en el inventario. */
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

        if (terreno == TipoDeTerreno.TEMPLO) {
            if (partida.yaFueExplorado(pos)) {
                return ResultadoAccion.TEMPLO_YA_EXPLORADO;
            }

            Reliquia reliquia = partida.obtenerReliquiaOculta(pos);
            if (reliquia != null) {
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