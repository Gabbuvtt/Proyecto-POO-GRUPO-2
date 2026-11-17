package controlador;

import modelo.AccionJuego;
import modelo.EstadoDePartida;
import modelo.FabricaAcciones;
import modelo.Participante;
import modelo.Partida;
import modelo.ResultadoAccion;
import servicio.PaqueteCofre;
import servicio.ServicioCriptografico;
import vista.VistaConsola;

import java.util.function.LongSupplier;

/**
 * Conecta la vista con el modelo: lee la opcion del jugador, le pide al modelo que la ejecute,
 * le dice a la vista que mostrar, mide el tiempo de cada turno y guarda la partida.
 * No tiene reglas del juego: eso lo decide el modelo.
 */
public class Controlador {

    private static final long MILISEGUNDOS_POR_SEGUNDO = 1000;

    private final VistaConsola vista;
    private final ServicioCriptografico criptografia;
    private final AlmacenPartida almacen;
    private final LongSupplier reloj;
    private long inicioTurno;

    /** Crea el controlador con el reloj real del computador. */
    public Controlador(VistaConsola vista, ServicioCriptografico criptografia, AlmacenPartida almacen) {
        this(vista, criptografia, almacen, System::currentTimeMillis);
    }

    /** Crea el controlador con un reloj dado (en milisegundos). Sirve para probar el cronometro. */
    public Controlador(VistaConsola vista, ServicioCriptografico criptografia, AlmacenPartida almacen,
                       LongSupplier reloj) {
        this.vista = vista;
        this.criptografia = criptografia;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /** Juega la partida turno a turno hasta que termine o el jugador guarde y salga. */
    public void jugar(Partida partida) {
        inicioTurno = reloj.getAsLong();
        boolean salir = false;
        while (!salir && partida.getEstado() == EstadoDePartida.EN_CURSO) {
            vista.mostrarEstadoTurno(partida);
            int opcion = vista.pedirOpcionTurno();
            salir = procesarOpcion(partida, opcion);
        }
        if (partida.getEstado() == EstadoDePartida.FINALIZADA) {
            vista.mostrarFinDePartida(partida);
        }
    }

    /** Hace lo que pidio el jugador. Devuelve true si hay que salir del juego. */
    private boolean procesarOpcion(Partida partida, int opcion) {
        switch (opcion) {
            case 1 -> ejecutar(partida, FabricaAcciones.crearAccionMover(vista.pedirCoordenadaDestino()));
            case 2 -> ejecutar(partida, FabricaAcciones.crearAccionExplorar());
            case 3 -> ejecutar(partida, FabricaAcciones.crearAccionEntregar());
            case 4 -> abrirCofre(partida);
            case 5 -> vista.mostrarConsulta(partida);
            case 6 -> guardarAhora(partida);
            case 7 -> terminarTurno(partida);
            case 0 -> {
                return guardarYSalir(partida);
            }
            default -> vista.mostrarError("Opción no válida.");
        }
        return false;
    }

    /** Ejecuta una accion en el modelo, muestra el resultado y cierra el turno si se acabaron las acciones. */
    private void ejecutar(Partida partida, AccionJuego accion) {
        Participante actor = partida.getTurnoActual();
        ResultadoAccion resultado = partida.ejecutarAccion(accion);
        boolean turnoCerrado = elTurnoSeCerro(partida, actor);
        vista.mostrarResultado(resultado, turnoCerrado ? 0 : partida.getAccionesRestantes());
        if (turnoCerrado) {
            cerrarTurno(partida, actor);
        }
    }

    private void abrirCofre(Partida partida) {
        Participante actual = partida.getTurnoActual();
        int restantes = partida.getAccionesRestantes();
        if (partida.estaCofreAbierto()) {
            vista.mostrarResultado(ResultadoAccion.COFRE_YA_ABIERTO, restantes);
            return;
        }
        char terrenoActual = partida.getTablero().obtenerCasilla(actual.getPosicion()).getTipo().getSimbolo();
        if (terrenoActual != 'X') {
            vista.mostrarResultado(ResultadoAccion.NO_ESTA_EN_COFRE, restantes);
            return;
        }
        if (partida.getPistasDescubiertas().size() < 2) {
            vista.mostrarResultado(ResultadoAccion.FALTAN_PISTAS, restantes);
            return;
        }

        String contrasena = vista.pedirContrasenaCofre(partida);
        PaqueteCofre paquete = obtenerPaqueteSinFallar(partida.getPartidaId());
        Participante actor = partida.getTurnoActual();
        ResultadoAccion resultado = partida.ejecutarAccion(
                FabricaAcciones.crearAccionAbrirCofre(paquete, contrasena, criptografia));
        boolean turnoCerrado = elTurnoSeCerro(partida, actor);
        int restantes = turnoCerrado ? 0 : partida.getAccionesRestantes();
        if (resultado == ResultadoAccion.OK) {
            vista.mostrarCofreAbierto(restantes);
        } else {
            vista.mostrarResultado(resultado, restantes);
        }
        if (turnoCerrado) {
            cerrarTurno(partida, actor);
        }
    }

    /** Termina el turno antes de gastar las 3 acciones. */
    private void terminarTurno(Partida partida) {
        Participante actor = partida.getTurnoActual();
        partida.cambiarTurno();
        vista.mostrarMensaje("Turno terminado.");
        cerrarTurno(partida, actor);
    }

    /** Guarda la partida sin cambiar el turno. */
    private void guardarAhora(Partida partida) {
        sumarTiempoDelTurno(partida.getTurnoActual());
        if (intentarGuardar(partida)) {
            vista.mostrarMensaje("Partida guardada.");
        } else {
            vista.mostrarError("No se pudo guardar la partida. Intenta de nuevo.");
        }
    }

    /** Guarda y solo permite salir si la API confirmo el guardado. */
    private boolean guardarYSalir(Partida partida) {
        sumarTiempoDelTurno(partida.getTurnoActual());
        if (intentarGuardar(partida)) {
            vista.mostrarMensaje("Partida guardada. ¡Hasta pronto!");
            return true;
        }
        vista.mostrarError("No se pudo guardar la partida, por eso no se cerró el juego. Intenta de nuevo.");
        return false;
    }

    /** El turno se cerro si ahora le toca a otro participante o si la partida termino. */
    private boolean elTurnoSeCerro(Partida partida, Participante actor) {
        return partida.getTurnoActual() != actor || partida.getEstado() == EstadoDePartida.FINALIZADA;
    }

    /** Cierra un turno: suma su tiempo, empieza a medir el siguiente y guarda automaticamente. */
    private void cerrarTurno(Partida partida, Participante actor) {
        sumarTiempoDelTurno(actor);
        inicioTurno = reloj.getAsLong();
        if (intentarGuardar(partida)) {
            vista.mostrarMensaje("Turno guardado.");
        } else {
            vista.mostrarError("No se pudo guardar el turno. Usa la opción Guardar para reintentar.");
        }
    }

    /** Suma al participante los segundos completos que lleva su turno, sin perder los sobrantes. */
    private void sumarTiempoDelTurno(Participante actor) {
        long segundos = (reloj.getAsLong() - inicioTurno) / MILISEGUNDOS_POR_SEGUNDO;
        actor.sumarSegundos(segundos);
        inicioTurno = inicioTurno + segundos * MILISEGUNDOS_POR_SEGUNDO;
    }

    /** Guarda; si el almacen lanza un error, lo cuenta como que no se guardo. */
    private boolean intentarGuardar(Partida partida) {
        try {
            return almacen.guardar(partida);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Pide el paquete del cofre; si el almacen falla, devuelve null. */
    private PaqueteCofre obtenerPaqueteSinFallar(String partidaId) {
        try {
            return almacen.obtenerPaqueteCofre(partidaId);
        } catch (RuntimeException e) {
            return null;
        }
    }
}