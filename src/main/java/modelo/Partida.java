package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** Gestiona el estado general de una partida, el tablero, los turnos y los participantes. */
public class Partida {
    private String partidaId;
    private Tablero tablero;
    private Participante participante1;
    private Participante participante2;
    private Participante turnoActual;
    private int rondaActual;
    private int accionesRestantes;
    private EstadoDePartida estado;
    private boolean cofreAbierto;

    private HashSet<String> templosExplorados;
    private ArrayList<Pista> pistasDescubiertas;
    private HashMap<String, Reliquia> reliquiasOcultas;
    private HashMap<String, Pista> pistasOcultas;

    /**
     * Crea una nueva partida recibiendo a los participantes en el orden de turno sorteado.
     * El primer participante recibido (participante1) inicia el primer turno de la ronda 1.
     */
    public Partida(String partidaId, Tablero tablero, Participante participante1, Participante participante2) {
        this.partidaId = partidaId;
        this.tablero = tablero;
        this.participante1 = participante1;
        this.participante2 = participante2;

        this.rondaActual = 1;
        this.turnoActual = participante1;
        this.accionesRestantes = ReglasDelJuego.ACCIONES_POR_TURNO;
        this.estado = EstadoDePartida.EN_CURSO;
        this.cofreAbierto = false;

        this.templosExplorados = new HashSet<>();
        this.pistasDescubiertas = new ArrayList<>();
        this.reliquiasOcultas = new HashMap<>();
        this.pistasOcultas = new HashMap<>();
    }

    /**
     * Crea una nueva partida ubicando como participante1 al jugador cuyo ID salio sorteado por la API.
     */
    public Partida(String partidaId, Tablero tablero, Participante pA, Participante pB, String jugadorInicialId) {
        this(
                partidaId,
                tablero,
                (pB != null && pB.getJugadorId().equals(jugadorInicialId)) ? pB : pA,
                (pB != null && pB.getJugadorId().equals(jugadorInicialId)) ? pA : pB
        );
    }

    /** Devuelve el identificador unico de la partida. */
    public String getPartidaId() {
        return partidaId;
    }

    /** Devuelve el tablero de la partida. */
    public Tablero getTablero() {
        return tablero;
    }

    /** Devuelve el primer participante en el orden de turnos. */
    public Participante getParticipante1() {
        return participante1;
    }

    /** Devuelve el segundo participante en el orden de turnos. */
    public Participante getParticipante2() {
        return participante2;
    }

    /** Devuelve el participante que tiene el turno activo. */
    public Participante getTurnoActual() {
        return turnoActual;
    }

    /** Devuelve el numero de la ronda actual (1 a 8). */
    public int getRondaActual() {
        return rondaActual;
    }

    /** Devuelve las acciones restantes en el turno actual. */
    public int getAccionesRestantes() {
        return accionesRestantes;
    }

    /** Devuelve el estado actual de la partida (EN_CURSO o FINALIZADA). */
    public EstadoDePartida getEstado() {
        return estado;
    }

    /** Devuelve la lista de pistas descubiertas por los jugadores. */
    public ArrayList<Pista> getPistasDescubiertas() {
        return pistasDescubiertas;
    }

    /** Registra una reliquia oculta en la coordenada indicada del mapa. */
    public void esconderReliquia(Coordenada coord, Reliquia reliquia) {
        reliquiasOcultas.put(coord.getTexto(), reliquia);
    }

    /** Registra una pista oculta en la coordenada indicada del mapa. */
    public void esconderPista(Coordenada coord, Pista pista) {
        pistasOcultas.put(coord.getTexto(), pista);
    }

    /** Obtiene la reliquia oculta en una coordenada, o null si no hay. */
    public Reliquia obtenerReliquiaOculta(Coordenada coord) {
        return reliquiasOcultas.get(coord.getTexto());
    }

    /** Obtiene la pista oculta en una coordenada, o null si no hay. */
    public Pista obtenerPistaOculta(Coordenada coord) {
        return pistasOcultas.get(coord.getTexto());
    }

    /** Indica si el templo en la coordenada dada ya fue explorado. */
    public boolean yaFueExplorado(Coordenada coord) {
        return templosExplorados.contains(coord.getTexto());
    }

    /** Marca la coordenada de un templo como explorada. */
    public void marcarComoExplorado(Coordenada coord) {
        templosExplorados.add(coord.getTexto());
    }

    /** Agrega una nueva pista a la coleccion de pistas compartidas descubiertas. */
    public void agregarPistaDescubierta(Pista pista) {
        if (pista != null) {
            pistasDescubiertas.add(pista);
        }
    }

    /** Indica si los participantes ya reunieron todas las pistas requeridas para el cofre. */
    public boolean tienenTodasLasPistas() {
        return pistasDescubiertas.size() >= ReglasDelJuego.CANTIDAD_PISTAS;
    }

    /** Ejecuta una accion sobre la partida y devuelve el resultado obtenido. */
    public ResultadoAccion ejecutarAccion(AccionJuego accion) {
        if (accion == null) {
            throw new IllegalArgumentException("La accion no puede ser nula");
        }
        return accion.ejecutar(this);
    }

    /** Resta las acciones consumidas y cambia el turno si el presupuesto llega a 0. */
    public void consumirAcciones(int costo) {
        this.accionesRestantes = this.accionesRestantes - costo;

        if (this.accionesRestantes <= 0) {
            cambiarTurno();
        }
    }

    /** Pasa el turno al siguiente participante o avanza la ronda (finalizando al terminar la ronda 8). */
    public void cambiarTurno() {
        if (this.estado == EstadoDePartida.FINALIZADA) {
            return;
        }

        if (this.turnoActual == this.participante1) {
            this.turnoActual = this.participante2;
            this.accionesRestantes = ReglasDelJuego.ACCIONES_POR_TURNO;
        } else {
            this.rondaActual = this.rondaActual + 1;

            if (this.rondaActual > ReglasDelJuego.MAX_RONDAS) {
                this.rondaActual = ReglasDelJuego.MAX_RONDAS;
                this.accionesRestantes = 0;
                this.estado = EstadoDePartida.FINALIZADA;
            } else {
                this.turnoActual = this.participante1;
                this.accionesRestantes = ReglasDelJuego.ACCIONES_POR_TURNO;
            }
        }
    }

    /** Indica si el cofre de la partida ya fue abierto. */
    public boolean estaCofreAbierto() {
        return cofreAbierto;
    }

    /** Marca el cofre de la partida como abierto. */
    public void abrirCofre() {
        this.cofreAbierto = true;
    }

    /**
     * Devuelve la lista con el participante ganador (por puntos o desempate de reliquias entregadas),
     * o ambos participantes si hay empate total (victoria compartida).
     */
    public ArrayList<Participante> obtenerGanadores() {
        ArrayList<Participante> ganadores = new ArrayList<>();
        int puntos1 = participante1.getPuntos();
        int puntos2 = participante2.getPuntos();

        if (puntos1 > puntos2) {
            ganadores.add(participante1);
        } else if (puntos2 > puntos1) {
            ganadores.add(participante2);
        } else {
            int reliquias1 = participante1.getEntregadas().size();
            int reliquias2 = participante2.getEntregadas().size();

            if (reliquias1 > reliquias2) {
                ganadores.add(participante1);
            } else if (reliquias2 > reliquias1) {
                ganadores.add(participante2);
            } else {
                ganadores.add(participante1);
                ganadores.add(participante2);
            }
        }
        return ganadores;
    }

    /**
     * Devuelve 0 si juega el primer participante
     * o 1 si juega el segundo participante.
     */
    public int getIndiceJugadorActual() {
        if (turnoActual == participante1) {
            return 0;
        }
        return 1;
    }

    /**
     * Devuelve las coordenadas de los templos explorados.
     */
    public java.util.ArrayList<String> getTemplosExplorados() {
        return new java.util.ArrayList<>(templosExplorados);
    }

}