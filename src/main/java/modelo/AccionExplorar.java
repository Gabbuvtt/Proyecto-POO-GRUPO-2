
package modelo;

/**
 * Accion de explorar un templo para recolectar reliquias y descubrir pistas
 */
public class AccionExplorar implements AccionJuego {

    private final Reliquia reliquiaApi;
    private final Pista pistaApi;
    private final boolean usarDatosApi;

    /**
     * Constructor original, conservado temporalmente
     */
    public AccionExplorar() {
        this.reliquiaApi = null;
        this.pistaApi = null;
        this.usarDatosApi = false;
    }

    /**
     * Crea una exploracion con los datos obtenidos de la API.
     */
    public AccionExplorar(Reliquia reliquia, Pista pista) {
        if (reliquia == null) {
            throw new IllegalArgumentException(
                    "La API debe proporcionar una reliquia."
            );
        }

        this.reliquiaApi = reliquia;
        this.pistaApi = pista;
        this.usarDatosApi = true;
    }

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

        Reliquia reliquia = usarDatosApi
                ? reliquiaApi
                : partida.obtenerReliquiaOculta(pos);

        Pista pista = usarDatosApi
                ? pistaApi
                : partida.obtenerPistaOculta(pos);

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
