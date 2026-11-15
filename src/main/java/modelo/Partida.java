package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Partida {
    private String partidaId;
    private Tablero tablero;
    private Participante participante1;
    private Participante participante2;
    private Participante turnoActual;
    private int rondaActual;
    private int accionesRestantes;
    private EstadoDePartida estado;

    private HashSet<String> templosExplorados;
    private ArrayList<Pista> pistasDescubiertas;
    private HashMap<String, Reliquia> reliquiasOcultas;
    private HashMap<String, Pista> pistasOcultas;

    public Partida(String partidaId, Tablero tablero, Participante participante1, Participante participante2) {
        this.partidaId = partidaId;
        this.tablero = tablero;
        this.participante1 = participante1;
        this.participante2 = participante2;

        this.rondaActual = 1;
        this.turnoActual = participante1;
        this.accionesRestantes = ReglasDelJuego.getAccionesPorTurno();
        this.estado = EstadoDePartida.EN_CURSO;

        this.templosExplorados = new HashSet<>();
        this.pistasDescubiertas = new ArrayList<>();
        this.reliquiasOcultas = new HashMap<>();
        this.pistasOcultas = new HashMap<>();
    }

    // --- GETTERS BÁSICOS ---
    public String getPartidaId() {
        return partidaId;
    }

    public Tablero getTablero() {
        return tablero;
    }

    public Participante getParticipante1() {
        return participante1;
    }

    public Participante getParticipante2() {
        return participante2;
    }

    public Participante getTurnoActual() {
        return turnoActual;
    }

    public int getRondaActual() {
        return rondaActual;
    }

    public int getAccionesRestantes() {
        return accionesRestantes;
    }

    public EstadoDePartida getEstado() {
        return estado;
    }

    public ArrayList<Pista> getPistasDescubiertas() {
        return pistasDescubiertas;
    }

    // --- MÉTODOS PARA ESCONDER Y BUSCAR EN EL MAPA ---
    public void esconderReliquia(Coordenada coord, Reliquia reliquia) {
        reliquiasOcultas.put(coord.getTexto(), reliquia);
    }

    public void esconderPista(Coordenada coord, Pista pista) {
        pistasOcultas.put(coord.getTexto(), pista);
    }

    public Reliquia obtenerReliquiaOculta(Coordenada coord) {
        return reliquiasOcultas.get(coord.getTexto());
    }

    public Pista obtenerPistaOculta(Coordenada coord) {
        return pistasOcultas.get(coord.getTexto());
    }

    // --- CONTROL DE TEMPLOS Y PISTAS COMPARTIDAS ---
    public boolean yaFueExplorado(Coordenada coord) {
        return templosExplorados.contains(coord.getTexto());
    }

    public void marcarComoExplorado(Coordenada coord) {
        templosExplorados.add(coord.getTexto());
    }

    public void agregarPistaDescubierta(Pista pista) {
        if (pista != null) {
            pistasDescubiertas.add(pista);
        }
    }

    public boolean tienenTodasLasPistas() {
        if (pistasOcultas.size() > 0 && pistasDescubiertas.size() >= pistasOcultas.size()) {
            return true;
        } else {
            return false;
        }
    }

    // --- POLIMORFISMO: EJECUTAR CUALQUIER ACCIÓN ---
    public boolean ejecutarAccion(AccionJuego accion) {
        if (accion == null) {
            return false;
        }
        return accion.ejecutar(this);
    }

    // --- CONTROL DE TURNOS Y RONDAS ---
    public void consumirAcciones(int costo) {
        this.accionesRestantes = this.accionesRestantes - costo;

        if (this.accionesRestantes <= 0) {
            cambiarTurno();
        }
    }

    public void cambiarTurno() {
        if (this.estado == EstadoDePartida.FINALIZADA) {
            return;
        }

        if (this.turnoActual == this.participante1) {
            this.turnoActual = this.participante2;
            this.accionesRestantes = ReglasDelJuego.getAccionesPorTurno();
        } else {
            this.rondaActual = this.rondaActual + 1;

            if (this.rondaActual > ReglasDelJuego.getMaxRondas()) {
                this.rondaActual = ReglasDelJuego.getMaxRondas();
                this.accionesRestantes = 0;
                this.estado = EstadoDePartida.FINALIZADA;
            } else {
                this.turnoActual = this.participante1;
                this.accionesRestantes = ReglasDelJuego.getAccionesPorTurno();
            }
        }
    }

    public void finalizarPartidaPorCofre() {
        this.estado = EstadoDePartida.FINALIZADA;
        this.accionesRestantes = 0;
    }

    // Ver Quien gano y desempate
    public String obtenerGanador() {
        int puntos1 = participante1.getPuntos();
        int puntos2 = participante2.getPuntos();

        if (puntos1 > puntos2) {
            return "Ganador: " + participante1.getJugador().getNombre() + " con " + puntos1 + " puntos.";
        } else if (puntos2 > puntos1) {
            return "Ganador: " + participante2.getJugador().getNombre() + " con " + puntos2 + " puntos.";
        } else {
            int reliquias1 = participante1.getEntregadas().size();
            int reliquias2 = participante2.getEntregadas().size();

            if (reliquias1 > reliquias2) {
                return "Ganador por desempate (mas reliquias entregadas): " + participante1.getJugador().getNombre();
            } else if (reliquias2 > reliquias1) {
                return "Ganador por desempate (mas reliquias entregadas): " + participante2.getJugador().getNombre();
            } else {
                return "Victoria compartida (Empate total en puntos y reliquias).";
            }
        }
    }
}