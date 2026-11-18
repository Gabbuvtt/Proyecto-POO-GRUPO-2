
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
 * Conecta la vista con el modelo.
 * Procesa las opciones, mide los turnos y guarda la partida.
 */
public class Controlador {

    private static final long MILISEGUNDOS_POR_SEGUNDO = 1000;

    private final VistaConsola vista;
    private final ServicioCriptografico criptografia;
    private final AlmacenPartida almacen;
    private final LongSupplier reloj;

    private long inicioTurno;

    // Impide continuar jugando si un guardado falla.
    private boolean guardadoPendiente = false;

    /** Constructor que utiliza el reloj del computador. */
    public Controlador(
            VistaConsola vista,
            ServicioCriptografico criptografia,
            AlmacenPartida almacen) {

        this(vista, criptografia, almacen,
                System::currentTimeMillis);
    }

    /** Constructor que permite utilizar un reloj de prueba. */
    public Controlador(
            VistaConsola vista,
            ServicioCriptografico criptografia,
            AlmacenPartida almacen,
            LongSupplier reloj) {

        this.vista = vista;
        this.criptografia = criptografia;
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /** Ejecuta el ciclo principal de una partida. */
    public void jugar(Partida partida) {

        inicioTurno = reloj.getAsLong();
        guardadoPendiente = false;

        boolean salir = false;

        while (!salir &&
                (partida.getEstado() == EstadoDePartida.EN_CURSO
                        || guardadoPendiente)) {

            if (guardadoPendiente) {
                vista.mostrarError(
                        "Hay un guardado pendiente. "
                                + "Debes guardar correctamente "
                                + "antes de continuar jugando."
                );
            }

            vista.mostrarEstadoTurno(partida);

            int opcion = vista.pedirOpcionTurno();

            salir = procesarOpcion(partida, opcion);
        }

        if (partida.getEstado() == EstadoDePartida.FINALIZADA
                && !guardadoPendiente) {
            vista.mostrarFinDePartida(partida);
        }
    }

    /** Procesa la opcion elegida por el jugador. */
    private boolean procesarOpcion(Partida partida, int opcion) {

        // Si falla el guardado, solamente se permite
        // consultar, reintentar guardar o guardar y salir.
        if (guardadoPendiente
                && opcion != 5
                && opcion != 6
                && opcion != 0) {

            vista.mostrarError(
                    "No puedes realizar acciones mientras "
                            + "haya un guardado pendiente. "
                            + "Usa la opcion 6 para reintentar."
            );

            return false;
        }

        switch (opcion) {

            case 1 -> mover(partida);

            case 2 -> ejecutar(
                    partida,
                    FabricaAcciones.crearAccionExplorar()
            );

            case 3 -> ejecutar(
                    partida,
                    FabricaAcciones.crearAccionEntregar()
            );

            case 4 -> abrirCofre(partida);

            case 5 -> vista.mostrarConsulta(partida);

            case 6 -> guardarAhora(partida);

            case 7 -> terminarTurno(partida);

            case 0 -> {
                return guardarYSalir(partida);
            }

            default -> vista.mostrarError(
                    "Opcion no valida."
            );
        }

        return false;
    }

    /** Mueve al jugador y muestra su posicion real. */
    private void mover(Partida partida) {

        Participante jugador = partida.getTurnoActual();

        ejecutar(
                partida,
                FabricaAcciones.crearAccionMover(
                        vista.pedirCoordenadaDestino()
                )
        );

        vista.mostrarMensaje(
                "Posicion actual: "
                        + jugador.getPosicion().getTexto()
        );
    }

    /**
     * Ejecuta una accion y comprueba si el turno termino.
     */
    private void ejecutar(Partida partida, AccionJuego accion) {

        Participante actor = partida.getTurnoActual();

        ResultadoAccion resultado =
                partida.ejecutarAccion(accion);

        boolean turnoCerrado =
                elTurnoSeCerro(partida, actor);

        vista.mostrarResultado(
                resultado,
                turnoCerrado
                        ? 0
                        : partida.getAccionesRestantes()
        );

        if (turnoCerrado) {
            cerrarTurno(partida, actor);
        }
    }

    /**
     * Intenta abrir el cofre..
     */
    private void abrirCofre(Partida partida) {

        Participante actual = partida.getTurnoActual();

        int restantes = partida.getAccionesRestantes();

        if (partida.estaCofreAbierto()) {

            vista.mostrarResultado(
                    ResultadoAccion.COFRE_YA_ABIERTO,
                    restantes
            );

            return;
        }

        char terrenoActual = partida.getTablero()
                .obtenerCasilla(actual.getPosicion())
                .getTipo()
                .getSimbolo();

        if (terrenoActual != 'X') {

            vista.mostrarResultado(
                    ResultadoAccion.NO_ESTA_EN_COFRE,
                    restantes
            );

            return;
        }

        if (partida.getPistasDescubiertas().size() < 2) {

            vista.mostrarResultado(
                    ResultadoAccion.FALTAN_PISTAS,
                    restantes
            );

            return;
        }


        if (partida.getAccionesRestantes() <= 0) {
            vista.mostrarResultado(
                    ResultadoAccion.SIN_ACCIONES,
                    partida.getAccionesRestantes()
            );
            return;
        }

        if (actual.getInventario().getReliquias().size() >= 2) {
            vista.mostrarResultado(
                    ResultadoAccion.INVENTARIO_LLENO,
                    partida.getAccionesRestantes()
            );
            return;
        }


        String contrasena =
                vista.pedirContrasenaCofre(partida);

        PaqueteCofre paquete;

        try {

            paquete = almacen.obtenerPaqueteCofre(
                    partida.getPartidaId()
            );

        } catch (RuntimeException e) {

            vista.mostrarError(
                    "No se pudo consultar el cofre en la API: "
                            + e.getMessage()
            );

            return;
        }

        if (paquete == null) {

            vista.mostrarError(
                    "La API no devolvio el paquete del cofre."
            );

            return;
        }

        Participante actor = partida.getTurnoActual();

        ResultadoAccion resultado =
                partida.ejecutarAccion(
                        FabricaAcciones.crearAccionAbrirCofre(
                                paquete,
                                contrasena,
                                criptografia
                        )
                );

        boolean turnoCerrado =
                elTurnoSeCerro(partida, actor);

        restantes = turnoCerrado
                ? 0
                : partida.getAccionesRestantes();

        if (resultado == ResultadoAccion.OK) {

            vista.mostrarCofreAbierto(restantes);

        } else {

            vista.mostrarResultado(
                    resultado,
                    restantes
            );
        }

        if (turnoCerrado) {
            cerrarTurno(partida, actor);
        }
    }

    /** Termina el turno voluntariamente. */
    private void terminarTurno(Partida partida) {

        Participante actor = partida.getTurnoActual();

        partida.cambiarTurno();

        vista.mostrarMensaje("Turno terminado.");

        cerrarTurno(partida, actor);
    }

    /**
     * Guarda sin cambiar el turno.
     */
    private void guardarAhora(Partida partida) {

        boolean eraReintento = guardadoPendiente;

        if (!eraReintento
                && partida.getEstado() == EstadoDePartida.EN_CURSO) {

            sumarTiempoDelTurno(
                    partida.getTurnoActual()
            );
        }

        if (intentarGuardar(partida)) {

            if (eraReintento) {
                inicioTurno = reloj.getAsLong();
            }

            vista.mostrarMensaje(
                    "Partida guardada correctamente."
            );

        } else {

            vista.mostrarError(
                    "No se pudo guardar la partida. "
                            + "Las acciones estan bloqueadas "
                            + "hasta que el guardado funcione."
            );
        }
    }

    /**
     * Guarda y sale solamente si el almacen
     * confirma el guardado.
     */
    private boolean guardarYSalir(Partida partida) {

        boolean eraReintento = guardadoPendiente;

        if (!eraReintento
                && partida.getEstado() == EstadoDePartida.EN_CURSO) {

            sumarTiempoDelTurno(
                    partida.getTurnoActual()
            );
        }

        if (intentarGuardar(partida)) {

            vista.mostrarMensaje(
                    "Partida guardada. ¡Hasta pronto!"
            );

            return true;
        }

        vista.mostrarError(
                "No se pudo guardar la partida, "
                        + "por eso no se cerró el juego. "
                        + "Intenta de nuevo."
        );

        return false;
    }

    /**
     * Detecta cuando cambia el participante
     */
    private boolean elTurnoSeCerro(
            Partida partida,
            Participante actor) {

        return partida.getTurnoActual() != actor
                || partida.getEstado()
                == EstadoDePartida.FINALIZADA;
    }

    /**
     * Cierra el turn
     */
    private void cerrarTurno(
            Partida partida,
            Participante actor) {

        sumarTiempoDelTurno(actor);

        inicioTurno = reloj.getAsLong();

        if (intentarGuardar(partida)) {

            vista.mostrarMensaje(
                    "Turno guardado correctamente."
            );

        } else {

            vista.mostrarError(
                    "No se pudo guardar el turno. "
                            + "No podras continuar hasta "
                            + "reintentar el guardado."
            );
        }
    }

    /**
     * Acumula los segundos completos del turno
     */
    private void sumarTiempoDelTurno(
            Participante actor) {

        long tiempoTranscurrido =
                reloj.getAsLong() - inicioTurno;

        long segundos =
                Math.max(0, tiempoTranscurrido)
                        / MILISEGUNDOS_POR_SEGUNDO;

        actor.sumarSegundos(segundos);

        inicioTurno = inicioTurno
                + segundos * MILISEGUNDOS_POR_SEGUNDO;
    }

    /**
     * Intenta guardar y actualiza el estado
     */
    private boolean intentarGuardar(Partida partida) {

        boolean guardadoCorrecto;

        try {

            guardadoCorrecto = almacen.guardar(partida);

        } catch (RuntimeException e) {

            guardadoCorrecto = false;
        }

        guardadoPendiente = !guardadoCorrecto;

        return guardadoCorrecto;
    }
}
