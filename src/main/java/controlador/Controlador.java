
package controlador;

import modelo.AccionAbrirCofre;
import modelo.AccionJuego;
import modelo.Coordenada;
import modelo.EstadoDePartida;
import modelo.FabricaAcciones;
import modelo.Jugador;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.Reliquia;
import modelo.ResultadoAccion;
import modelo.TipoDeTerreno;
import servicio.FalloAutenticacionException;
import servicio.PaqueteCofre;
import servicio.PaqueteCorruptoException;
import servicio.ServicioCriptografico;
import servicio.VectorAnexoB;
import vista.DatosRegistro;
import vista.VistaConsola;

import java.util.ArrayList;
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

    // la API tiene una revision mas nueva, ya no se puede guardar
    private boolean conflictoVersion = false;

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

    /** Comprueba la API y muestra el menu principal hasta que el usuario sale. */
    public void iniciar() {

        if (!almacen.apiDisponible()) {
            vista.mostrarError(
                    "La API no esta disponible o usa otra version "
                            + "del contrato. Iniciala e intenta de nuevo."
            );
            return;
        }

        boolean salir = false;

        while (!salir) {

            int opcion = vista.pedirOpcionMenuPrincipal();

            // si falla una consulta se avisa y se vuelve al menu
            try {
                switch (opcion) {
                    case 1 -> registrarJugador();
                    case 2 -> nuevaPartida();
                    case 3 -> cargarPartida();
                    case 4 -> verEstadisticas();
                    case 5 -> ayuda();
                    default -> salir = true;
                }
            } catch (RuntimeException e) {
                vista.mostrarError(e.getMessage());
            }
        }
    }

    /** Pide nombre y alias y registra al jugador en la API. */
    private void registrarJugador() {
        DatosRegistro datos = vista.pedirDatosRegistro();
        String jugadorId = almacen.registrarJugador(
                datos.nombre(), datos.alias()
        );
        vista.mostrarJugadorCreado(jugadorId);
    }

    /** Pide los dos jugadores, crea la partida y empieza a jugar. */
    private void nuevaPartida() {
        vista.mostrarMensaje("");
        vista.mostrarMensaje("Nueva partida");

        Jugador jugador1 = pedirJugador("Jugador 1: ");
        if (jugador1 == null) {
            return;
        }

        Jugador jugador2 = pedirJugador("Jugador 2: ");
        if (jugador2 == null) {
            return;
        }

        if (jugador1.getJugadorId().equals(jugador2.getJugadorId())) {
            vista.mostrarError("Los dos jugadores deben ser distintos.");
            return;
        }

        String partidaId = almacen.crearPartida(
                jugador1.getJugadorId(), jugador2.getJugadorId()
        );

        // se trae completa para tener el campamento y la revision
        Partida partida = almacen.cargarPartida(partidaId);
        vista.mostrarPartidaCreada(partida);
        jugar(partida);
    }

    /** Lista las partidas de un jugador y abre la que elija. */
    private void cargarPartida() {
        Jugador jugador = pedirJugador("Alias: ");
        if (jugador == null) {
            return;
        }

        ArrayList<ResumenPartida> partidas =
                almacen.listarPartidas(jugador.getJugadorId());

        if (partidas.isEmpty()) {
            vista.mostrarMensaje(jugador.getNombre() + " no tiene partidas.");
            return;
        }

        ArrayList<String> lineas = new ArrayList<>();
        for (ResumenPartida resumen : partidas) {
            lineas.add(resumen.getPartidaId()
                    + " | " + resumen.getJugadores()
                    + " | " + resumen.getEstado()
                    + " | Ronda " + resumen.getRonda());
        }

        int elegida = vista.elegirDeLista("Partidas de " + jugador.getNombre(), lineas);
        if (elegida == 0) {
            return;
        }

        // una partida finalizada solo muestra su resultado
        Partida partida = almacen.cargarPartida(
                partidas.get(elegida - 1).getPartidaId()
        );
        jugar(partida);
    }

    /** Muestra las estadisticas de un jugador. */
    private void verEstadisticas() {
        Jugador jugador = pedirJugador("Alias: ");
        if (jugador == null) {
            return;
        }

        EstadisticasJugador datos =
                almacen.obtenerEstadisticas(jugador.getJugadorId());

        vista.mostrarEstadisticas(
                jugador.getNombre(),
                datos.getPartidasFinalizadas(),
                datos.getVictorias(),
                datos.getPuntosAcumulados(),
                datos.getReliquiasEntregadas()
        );
    }

    /** Muestra las reglas y deja probar el vector del Anexo B. */
    private void ayuda() {
        vista.mostrarAyuda();

        if (vista.pedirOpcionAyuda() == 1) {
            descifrarVectorAnexoB();
        }
    }

    /** Descifra el paquete fijo del Anexo B con su contrasena publica. */
    private void descifrarVectorAnexoB() {
        PaqueteCofre paquete = VectorAnexoB.crearPaquete();

        try {
            String textoClaro = criptografia.descifrar(
                    paquete, VectorAnexoB.CONTRASENA
            );

            vista.mostrarVectorAnexoB(
                    paquete.getPartidaId(),
                    paquete.getCofreId(),
                    VectorAnexoB.CONTRASENA,
                    textoClaro
            );

        } catch (FalloAutenticacionException | PaqueteCorruptoException e) {
            vista.mostrarError("No se pudo descifrar el vector: " + e.getMessage());
        }
    }

    /** Pide un alias y busca al jugador. Devuelve null si no existe. */
    private Jugador pedirJugador(String mensaje) {
        String alias = vista.pedirAlias(mensaje);
        Jugador jugador = almacen.buscarJugadorPorAlias(alias);

        if (jugador == null) {
            vista.mostrarError("No existe un jugador con el alias " + alias + ".");
        }

        return jugador;
    }

    /** Ejecuta el ciclo principal de una partida. */
    public void jugar(Partida partida) {

        inicioTurno = reloj.getAsLong();
        guardadoPendiente = false;
        conflictoVersion = false;

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

            case 2 -> explorar(partida);

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

    /** Mueve al jugador. Si el destino no es vecino lo dice desde donde esta. */
    private void mover(Partida partida) {

        Participante actor = partida.getTurnoActual();
        Coordenada origen = actor.getPosicion();

        ResultadoAccion resultado = partida.ejecutarAccion(
                FabricaAcciones.crearAccionMover(
                        vista.pedirCoordenadaDestino()
                )
        );

        boolean turnoCerrado = elTurnoSeCerro(partida, actor);
        int restantes = turnoCerrado ? 0 : partida.getAccionesRestantes();

        if (resultado == ResultadoAccion.NO_ES_VECINO) {
            vista.mostrarMovimientoInvalido(origen.getTexto(), restantes);
        } else {
            vista.mostrarResultado(resultado, restantes);
        }

        despuesDeAccion(partida, actor, resultado, turnoCerrado);
    }

    /**
     * Carga el templo desde la API si hace falta y luego explora.
     */
    private void explorar(Partida partida) {

        Coordenada posicion =
                partida.getTurnoActual().getPosicion();

        boolean esTemplo = partida.getTablero()
                .obtenerCasilla(posicion)
                .getTipo() == TipoDeTerreno.TEMPLO;

        // solo se pide si es un templo sin explorar y aun no cargado
        if (esTemplo
                && !partida.yaFueExplorado(posicion)
                && partida.obtenerReliquiaOculta(posicion) == null) {

            ContenidoTemplo contenido;

            try {

                contenido = almacen.obtenerContenidoTemplo(
                        partida.getPartidaId(),
                        posicion.getTexto()
                );

            } catch (RuntimeException e) {

                // no se ejecuta la accion, asi no se gasta
                vista.mostrarError(
                        "No se pudo consultar el templo en la API: "
                                + e.getMessage()
                );

                return;
            }

            if (contenido == null) {

                vista.mostrarError(
                        "La API no devolvio el contenido del templo."
                );

                return;
            }

            if (contenido.getReliquia() != null) {
                partida.esconderReliquia(
                        posicion, contenido.getReliquia()
                );
            }

            if (contenido.getPista() != null) {
                partida.esconderPista(
                        posicion, contenido.getPista()
                );
            }
        }

        // se leen antes de explorar para poder contar que se encontro
        Reliquia reliquia = partida.obtenerReliquiaOculta(posicion);
        Pista pista = partida.obtenerPistaOculta(posicion);
        Participante actor = partida.getTurnoActual();

        ResultadoAccion resultado = partida.ejecutarAccion(
                FabricaAcciones.crearAccionExplorar()
        );

        boolean turnoCerrado = elTurnoSeCerro(partida, actor);
        int restantes = turnoCerrado ? 0 : partida.getAccionesRestantes();

        if (resultado == ResultadoAccion.OK) {
            vista.mostrarExploracion(reliquia, pista, restantes);
        } else {
            vista.mostrarResultado(resultado, restantes);
        }

        despuesDeAccion(partida, actor, resultado, turnoCerrado);
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

        despuesDeAccion(partida, actor, resultado, turnoCerrado);
    }

    /**
     * Intenta abrir el cofre. Antes de pedir la contrasena
     * el modelo revisa todo lo que no depende de ella.
     */
    private void abrirCofre(Partida partida) {

        ResultadoAccion previo =
                AccionAbrirCofre.comprobarCondiciones(partida);

        if (previo != ResultadoAccion.OK) {
            vista.mostrarResultadoCofre(
                    previo,
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

        int restantes = turnoCerrado
                ? 0
                : partida.getAccionesRestantes();

        if (resultado == ResultadoAccion.OK) {

            vista.mostrarCofreAbierto(restantes);

        } else {

            vista.mostrarResultadoCofre(
                    resultado,
                    restantes
            );
        }

        despuesDeAccion(partida, actor, resultado, turnoCerrado);
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
     * confirma el guardado. Con conflicto de versiones sale sin guardar.
     */
    private boolean guardarYSalir(Partida partida) {

        // reintentar no sirve, la API ya tiene otra version
        if (conflictoVersion) {
            return salirSinGuardar();
        }

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

        if (conflictoVersion) {
            return salirSinGuardar();
        }

        vista.mostrarError(
                "No se pudo guardar la partida, "
                        + "por eso no se cerró el juego. "
                        + "Intenta de nuevo."
        );

        return false;
    }

    /** Vuelve al menu principal sin guardar cuando hay conflicto de versiones. */
    private boolean salirSinGuardar() {
        vista.mostrarMensaje(
                "Se vuelve al menu sin guardar. Los cambios desde el"
                        + " ultimo guardado se pierden. Carga la partida"
                        + " de nuevo para seguir desde la version de la API."
        );
        return true;
    }

    /**
     * Despues de una accion cierra el turno si se acabo.
     * Si no, guarda solo cuando la accion cambio el estado.
     */
    private void despuesDeAccion(
            Partida partida,
            Participante actor,
            ResultadoAccion resultado,
            boolean turnoCerrado) {

        if (turnoCerrado) {
            cerrarTurno(partida, actor);
            return;
        }

        // un fallo de contrasena tambien cobra, asi que se guarda
        boolean cambioElEstado = resultado == ResultadoAccion.OK
                || resultado == ResultadoAccion.FALLO_AUTENTICACION;

        if (!cambioElEstado) {
            return;
        }

        sumarTiempoDelTurno(actor);

        if (!intentarGuardar(partida)) {
            vista.mostrarError(
                    "No se pudo guardar la accion. "
                            + "No podras continuar hasta "
                            + "reintentar el guardado."
            );
        }
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
        conflictoVersion = false;

        try {

            guardadoCorrecto = almacen.guardar(partida);

        } catch (ConflictoVersionException e) {

            vista.mostrarError(
                    "Conflicto de versiones: " + e.getMessage()
                            + " Otro proceso guardo esta partida antes."
                            + " Tus cambios no se guardaron. Usa la opcion 0"
                            + " para volver al menu y carga la partida de nuevo."
            );

            conflictoVersion = true;
            guardadoCorrecto = false;

        } catch (RuntimeException e) {

            guardadoCorrecto = false;
        }

        guardadoPendiente = !guardadoCorrecto;

        return guardadoCorrecto;
    }
}
