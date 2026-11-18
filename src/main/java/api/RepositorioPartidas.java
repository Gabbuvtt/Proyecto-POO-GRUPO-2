
package api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controlador.AlmacenPartida;
import controlador.ConflictoVersionException;
import controlador.ContenidoTemplo;
import controlador.EstadisticasJugador;
import controlador.ResumenPartida;
import modelo.Coordenada;
import modelo.EstadoDePartida;
import modelo.Jugador;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.Reliquia;
import modelo.Tablero;
import modelo.TipoDeTerreno;
import servicio.PaqueteCofre;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Repositorio encargado de comunicarse con la API Python
 * para guardar partidas y consultar el cofre.
 *
 * Implementa AlmacenPartida para mantener separado
 * el controlador de los detalles de HTTP y JSON.
 */
public class RepositorioPartidas implements AlmacenPartida {

    private static final int VERSION_CONTRATO = 2;
    private static final int MAX_INTENTOS_CREAR = 3;

    private final ClienteApi cliente;
    private final ObjectMapper mapeador;
    private final Map<String, Integer> revisiones;

    /**
     * Construye el repositorio con el cliente HTTP recibido.
     */
    public RepositorioPartidas(ClienteApi cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException(
                    "El cliente API no puede ser null."
            );
        }

        this.cliente = cliente;
        this.mapeador = new ObjectMapper();
        this.revisiones = new HashMap<>();
    }

    /**
     * Registra la revision conocida de una partida.
     */
    public void registrarRevision(String partidaId, int revision) {
        if (partidaId == null || partidaId.isBlank()) {
            throw new IllegalArgumentException(
                    "El ID de partida no puede estar vacio."
            );
        }

        if (revision < 0) {
            throw new IllegalArgumentException(
                    "La revision no puede ser negativa."
            );
        }

        revisiones.put(partidaId, revision);
    }

    /**
     * Guarda el estado completo de la partida.
     *
     * Devuelve true solamente si la API confirma
     * el guardado o si una consulta posterior permite
     * verificar que el estado fue persistido.
     */
    @Override
    public boolean guardar(Partida partida) {
        if (partida == null) {
            throw new IllegalArgumentException(
                    "La partida no puede ser null."
            );
        }

        String partidaId = partida.getPartidaId();

        if (partidaId == null || partidaId.isBlank()) {
            throw new IllegalArgumentException(
                    "La partida necesita un identificador."
            );
        }

        int revisionBase = revisiones.getOrDefault(partidaId, 0);

        ObjectNode estado = construirEstado(partida);

        ObjectNode cuerpo = mapeador.createObjectNode();
        cuerpo.put("revisionBase", revisionBase);
        cuerpo.set("estado", estado);

        try {
            String respuesta = cliente.put(
                    "/partidas/" + partidaId + "/estado",
                    cuerpo.toString()
            );

            JsonNode confirmacion = mapeador.readTree(respuesta);

            if (confirmacion != null
                    && confirmacion.path("revision").isIntegralNumber()) {

                int revisionNueva =
                        confirmacion.get("revision").asInt();

                if (revisionNueva >= revisionBase + 1) {
                    revisiones.put(partidaId, revisionNueva);
                    return true;
                }
            }

            // La respuesta no confirma una revision nueva.
            // Intentamos verificar el estado consultando la API.
            return confirmarPorConsulta(
                    partidaId, revisionBase, estado
            );

        } catch (ClienteApi.ErrorApi e) {
            if (e.getCodigoEstado() == 409) {
                // puede ser un reintento que la API ya guardo
                if (confirmarPorConsulta(partidaId, revisionBase, estado)) {
                    return true;
                }

                throw new ConflictoVersionException(
                        "La partida tiene una version mas nueva en la API "
                                + "(revision base " + revisionBase + ")."
                );
            }

            return false;

        } catch (JsonProcessingException e) {
            return confirmarPorConsulta(
                    partidaId, revisionBase, estado
            );

        } catch (RuntimeException e) {
            // Puede haberse perdido la respuesta HTTP.
            return confirmarPorConsulta(
                    partidaId, revisionBase, estado
            );
        }
    }

    /**
     * Consulta el paquete cifrado del cofre.
     */
    @Override
    public PaqueteCofre obtenerPaqueteCofre(String partidaId) {
        String respuesta = cliente.get(
                "/partidas/" + partidaId + "/cofre"
        );

        try {
            JsonNode datos = mapeador.readTree(respuesta);

            if (datos == null || !datos.isObject()) {
                throw new IllegalStateException(
                        "La API no devolvio un objeto de cofre valido."
                );
            }

            return new PaqueteCofre(
                    obtenerTexto(datos, "partidaId"),
                    obtenerTexto(datos, "cofreId"),
                    obtenerTexto(datos, "algoritmo"),
                    obtenerTexto(datos, "kdf"),
                    obtenerEntero(datos, "iteraciones"),
                    obtenerEntero(datos, "longitudClaveBits"),
                    obtenerTexto(datos, "saltBase64"),
                    obtenerTexto(datos, "nonceBase64"),
                    obtenerEntero(datos, "tagBits"),
                    obtenerTexto(datos, "aadBase64"),
                    obtenerTexto(datos, "cifradoConTagBase64")
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "La API devolvio un JSON de cofre invalido.",
                    e
            );
        }
    }

    /**
     * Indica si la API responde y usa la version de contrato esperada.
     */
    @Override
    public boolean apiDisponible() {
        try {
            JsonNode salud = leerJson(cliente.get("/salud"));

            return salud.path("disponible").asBoolean(false)
                    && salud.path("versionContrato").asInt(-1)
                    == VERSION_CONTRATO;

        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Registra un jugador y devuelve su jugadorId.
     */
    @Override
    public String registrarJugador(String nombre, String alias) {
        ObjectNode cuerpo = mapeador.createObjectNode();
        cuerpo.put("nombre", nombre);
        cuerpo.put("alias", alias);

        String respuesta;

        try {
            respuesta = cliente.post("/jugadores", cuerpo.toString());
        } catch (ClienteApi.ErrorApi e) {
            throw new IllegalStateException(leerMensajeError(e), e);
        }

        return obtenerTexto(leerJson(respuesta), "jugadorId");
    }

    /**
     * Busca un jugador por su alias. Devuelve null si no existe.
     */
    @Override
    public Jugador buscarJugadorPorAlias(String alias) {
        JsonNode lista = leerJson(consultar(
                "/jugadores?alias="
                        + URLEncoder.encode(alias, StandardCharsets.UTF_8)
        ));

        if (!lista.isArray() || lista.isEmpty()) {
            return null;
        }

        return leerJugador(lista.get(0));
    }

    /**
     * Crea una partida para dos jugadores y devuelve su partidaId.
     * Si falla la red reintenta con el mismo requestId.
     */
    @Override
    public String crearPartida(String jugador1, String jugador2) {
        ObjectNode cuerpo = mapeador.createObjectNode();
        cuerpo.put("requestId", UUID.randomUUID().toString());
        ArrayNode jugadores = cuerpo.putArray("jugadores");
        jugadores.add(jugador1);
        jugadores.add(jugador2);

        String respuesta = null;
        RuntimeException ultimoError = null;

        for (int intento = 1;
             intento <= MAX_INTENTOS_CREAR && respuesta == null;
             intento++) {

            try {
                respuesta = cliente.post("/partidas", cuerpo.toString());
            } catch (ClienteApi.ErrorApi e) {
                // la API contesto con un error, reintentar no sirve
                throw new IllegalStateException(leerMensajeError(e), e);
            } catch (RuntimeException e) {
                // error de red, se repite el mismo cuerpo
                ultimoError = e;
            }
        }

        if (respuesta == null) {
            throw new IllegalStateException(
                    "No se pudo crear la partida, la API no responde.",
                    ultimoError
            );
        }

        return obtenerTexto(leerJson(respuesta), "partidaId");
    }

    /**
     * Lista las partidas en las que participa un jugador.
     */
    @Override
    public ArrayList<ResumenPartida> listarPartidas(String jugadorId) {
        JsonNode lista = leerJson(consultar(
                "/partidas?jugadorId="
                        + URLEncoder.encode(jugadorId, StandardCharsets.UTF_8)
        ));

        ArrayList<ResumenPartida> resumenes = new ArrayList<>();

        for (JsonNode dato : lista) {
            String alias = "";

            for (JsonNode jugador : dato.path("jugadores")) {
                if (!alias.isEmpty()) {
                    alias = alias + ", ";
                }
                alias = alias + jugador.path("alias").asText();
            }

            resumenes.add(new ResumenPartida(
                    obtenerTexto(dato, "partidaId"),
                    alias,
                    obtenerTexto(dato, "estado"),
                    obtenerEntero(dato, "ronda")
            ));
        }

        return resumenes;
    }

    /**
     * Consulta las estadisticas de un jugador.
     */
    @Override
    public EstadisticasJugador obtenerEstadisticas(String jugadorId) {
        JsonNode datos = leerJson(consultar(
                "/jugadores/" + jugadorId + "/estadisticas"
        ));

        return new EstadisticasJugador(
                obtenerEntero(datos, "partidasFinalizadas"),
                obtenerEntero(datos, "victorias"),
                obtenerEntero(datos, "puntosAcumulados"),
                obtenerEntero(datos, "reliquiasEntregadas")
        );
    }

    /**
     * Trae una partida guardada y la reconstruye con todo su estado.
     * No reinicia el turno: ronda, jugador y acciones quedan como se guardaron.
     */
    @Override
    public Partida cargarPartida(String partidaId) {
        JsonNode datos = leerJson(consultar("/partidas/" + partidaId));
        JsonNode estado = datos.path("estado");
        JsonNode jugadores = estado.path("jugadores");

        if (!jugadores.isArray() || jugadores.size() != 2) {
            throw new IllegalStateException(
                    "La partida guardada no tiene 2 jugadores."
            );
        }

        HashMap<String, Jugador> registrados = leerJugadores();

        // el orden de estado.jugadores es el orden de turnos
        Partida partida = new Partida(
                partidaId,
                leerTablero(datos.path("terreno")),
                leerParticipante(jugadores.get(0), registrados),
                leerParticipante(jugadores.get(1), registrados)
        );

        for (JsonNode templo : estado.path("templosExplorados")) {
            partida.marcarComoExplorado(new Coordenada(templo.asText()));
        }

        for (JsonNode pista : estado.path("pistas")) {
            partida.agregarPistaDescubierta(new Pista(
                    obtenerEntero(pista, "indice"),
                    obtenerTexto(pista, "fragmento")
            ));
        }

        partida.restaurarEstado(
                obtenerEntero(estado, "ronda"),
                obtenerEntero(estado, "indiceJugadorActual"),
                obtenerEntero(estado, "accionesRestantes"),
                EstadoDePartida.valueOf(obtenerTexto(estado, "estado")),
                estado.path("cofreAbierto").asBoolean(false)
        );

        registrarRevision(partidaId, obtenerEntero(datos, "revision"));

        return partida;
    }

    /**
     * Arma el tablero a partir de la matriz de letras de la API.
     */
    private Tablero leerTablero(JsonNode terreno) {
        if (!terreno.isArray() || terreno.isEmpty()) {
            throw new IllegalStateException(
                    "La API no devolvio el terreno."
            );
        }

        int filas = terreno.size();
        int columnas = terreno.get(0).size();
        Tablero tablero = new Tablero(filas, columnas);

        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                String letra = terreno.get(f).path(c).asText();
                TipoDeTerreno tipo = null;

                if (letra.length() == 1) {
                    tipo = TipoDeTerreno.buscarPorSimbolo(letra.charAt(0));
                }

                if (tipo == null) {
                    throw new IllegalStateException(
                            "Letra de terreno desconocida: " + letra
                    );
                }

                tablero.colocarTerreno(new Coordenada(f, c), tipo);
            }
        }

        return tablero;
    }

    /**
     * Crea un participante con posicion, inventario, puntos,
     * entregadas y segundos guardados.
     */
    private Participante leerParticipante(
            JsonNode dato,
            HashMap<String, Jugador> registrados) {

        String jugadorId = obtenerTexto(dato, "jugadorId");
        Jugador jugador = registrados.get(jugadorId);

        if (jugador == null) {
            throw new IllegalStateException(
                    "No existe el jugador " + jugadorId
            );
        }

        Participante participante = new Participante(
                jugador,
                new Coordenada(obtenerTexto(dato, "posicion"))
        );

        for (JsonNode reliquia : dato.path("inventario")) {
            participante.getInventario().agregarReliquia(
                    leerReliquia(reliquia)
            );
        }

        ArrayList<Reliquia> entregadas = new ArrayList<>();

        for (JsonNode reliquia : dato.path("entregadas")) {
            entregadas.add(leerReliquia(reliquia));
        }

        participante.restaurarPuntosYEntregadas(
                obtenerEntero(dato, "puntos"),
                entregadas
        );

        // si el estado es viejo y no lo trae, queda en 0
        participante.sumarSegundos(
                dato.path("segundosConsumidos").asLong(0)
        );

        return participante;
    }

    /**
     * Trae todos los jugadores registrados, guardados por jugadorId.
     */
    private HashMap<String, Jugador> leerJugadores() {
        JsonNode lista = leerJson(consultar("/jugadores"));
        HashMap<String, Jugador> jugadores = new HashMap<>();

        for (JsonNode dato : lista) {
            Jugador jugador = leerJugador(dato);
            jugadores.put(jugador.getJugadorId(), jugador);
        }

        return jugadores;
    }

    private Jugador leerJugador(JsonNode dato) {
        return new Jugador(
                obtenerTexto(dato, "jugadorId"),
                obtenerTexto(dato, "nombre"),
                obtenerTexto(dato, "alias")
        );
    }

    private Reliquia leerReliquia(JsonNode dato) {
        return new Reliquia(
                obtenerTexto(dato, "reliquiaId"),
                obtenerEntero(dato, "valor")
        );
    }

    /**
     * Hace un GET y cambia los errores de la API por un mensaje claro.
     */
    private String consultar(String ruta) {
        try {
            return cliente.get(ruta);
        } catch (ClienteApi.ErrorApi e) {
            // usa el mensaje que manda la API en {"codigo","mensaje"}
            throw new IllegalStateException(leerMensajeError(e), e);
        }
    }

    /**
     * Convierte el texto de la respuesta en JSON.
     */
    private JsonNode leerJson(String texto) {
        try {
            JsonNode datos = mapeador.readTree(texto);

            if (datos == null) {
                throw new IllegalStateException(
                        "La API devolvio una respuesta vacia."
                );
            }

            return datos;

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "La API devolvio un JSON invalido.", e
            );
        }
    }

    /**
     * Consulta la reliquia y la pista de un templo.
     */
    @Override
    public ContenidoTemplo obtenerContenidoTemplo(
            String partidaId,
            String coordenada) {

        String respuesta = consultar(
                "/partidas/" + partidaId
                        + "/templos/" + coordenada
        );

        try {
            JsonNode datos = mapeador.readTree(respuesta);

            if (datos == null || !datos.isObject()) {
                throw new IllegalStateException(
                        "La API no devolvio un templo valido."
                );
            }

            Reliquia reliquia = null;
            JsonNode datosReliquia = datos.get("reliquia");

            if (datosReliquia != null && datosReliquia.isObject()) {
                reliquia = new Reliquia(
                        obtenerTexto(datosReliquia, "reliquiaId"),
                        obtenerEntero(datosReliquia, "valor")
                );
            }

            Pista pista = null;
            JsonNode datosPista = datos.get("pista");

            if (datosPista != null && datosPista.isObject()) {
                pista = new Pista(
                        obtenerEntero(datosPista, "indice"),
                        obtenerTexto(datosPista, "fragmento")
                );
            }

            return new ContenidoTemplo(reliquia, pista);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "La API devolvio un JSON de templo invalido.",
                    e
            );
        }
    }

    /**
     * Saca el campo "mensaje" del error de la API.
     * Si no se puede leer, usa el mensaje del error HTTP.
     */
    private String leerMensajeError(ClienteApi.ErrorApi error) {
        try {
            JsonNode datos = mapeador.readTree(error.getRespuesta());

            if (datos != null && datos.path("mensaje").isTextual()) {
                return datos.get("mensaje").asText();
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // cuerpo vacio o que no es JSON
        }

        return error.getMessage();
    }

    /**
     * Consulta la API para verificar si se guardo
     * el estado aunque se haya perdido la respuesta.
     */
    private boolean confirmarPorConsulta(
            String partidaId,
            int revisionBase,
            ObjectNode estadoEsperado) {

        try {
            String respuesta = cliente.get(
                    "/partidas/" + partidaId
            );

            JsonNode remoto = mapeador.readTree(respuesta);

            if (remoto == null || !remoto.isObject()) {
                return false;
            }

            JsonNode revision = remoto.get("revision");
            JsonNode estadoRemoto = remoto.get("estado");

            if (revision == null
                    || !revision.isIntegralNumber()
                    || estadoRemoto == null
                    || !estadoRemoto.isObject()) {
                return false;
            }

            int revisionRemota = revision.asInt();

            if (revisionRemota == revisionBase + 1
                    && estadoEsperado.equals(estadoRemoto)) {

                revisiones.put(partidaId, revisionRemota);
                return true;
            }

            return false;

        } catch (JsonProcessingException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Convierte el estado del modelo Java al JSON
     * solicitado por el contrato de la API.
     */
    private ObjectNode construirEstado(Partida partida) {
        ObjectNode estado = mapeador.createObjectNode();

        estado.put("estado", partida.getEstado().name());
        estado.put("ronda", partida.getRondaActual());
        estado.put(
                "indiceJugadorActual",
                partida.getIndiceJugadorActual()
        );
        estado.put(
                "accionesRestantes",
                partida.getAccionesRestantes()
        );

        ArrayNode jugadores = estado.putArray("jugadores");

        agregarParticipante(
                jugadores, partida.getParticipante1()
        );

        agregarParticipante(
                jugadores, partida.getParticipante2()
        );

        ArrayNode templos = estado.putArray(
                "templosExplorados"
        );

        for (String coordenada : partida.getTemplosExplorados()) {
            templos.add(coordenada);
        }

        ArrayNode pistas = estado.putArray("pistas");

        for (Pista pista : partida.getPistasDescubiertas()) {
            ObjectNode dato = pistas.addObject();
            dato.put("indice", pista.getIndice());
            dato.put("fragmento", pista.getFragmento());
        }

        estado.put(
                "cofreAbierto",
                partida.estaCofreAbierto()
        );

        ArrayNode ganadores = estado.putArray("ganadores");

        if (partida.getEstado() == EstadoDePartida.FINALIZADA) {
            for (Participante ganador : partida.obtenerGanadores()) {
                ganadores.add(ganador.getJugadorId());
            }
        }

        return estado;
    }

    /**
     * Agrega los datos de un participante al JSON.
     */
    private void agregarParticipante(
            ArrayNode jugadores,
            Participante participante) {

        ObjectNode jugador = jugadores.addObject();

        jugador.put(
                "jugadorId",
                participante.getJugadorId()
        );

        jugador.put(
                "posicion",
                participante.getPosicion().getTexto()
        );

        jugador.put("puntos", participante.getPuntos());

        // tiempo acumulado en sus turnos. Va como int porque al leer la
        // respuesta Jackson crea un int, y un long no seria igual al comparar
        jugador.put(
                "segundosConsumidos",
                (int) participante.getSegundosConsumidos()
        );

        agregarReliquias(
                jugador.putArray("inventario"),
                participante.getInventario().getReliquias()
        );

        agregarReliquias(
                jugador.putArray("entregadas"),
                participante.getEntregadas()
        );
    }

    /**
     * Convierte una coleccion de reliquias en JSON.
     */
    private void agregarReliquias(
            ArrayNode destino,
            Iterable<Reliquia> reliquias) {

        for (Reliquia reliquia : reliquias) {
            ObjectNode dato = destino.addObject();

            dato.put(
                    "reliquiaId",
                    reliquia.getReliquiaId()
            );

            dato.put("valor", reliquia.getValor());
        }
    }

    /**
     * Lee un campo de texto obligatorio del JSON.
     */
    private String obtenerTexto(JsonNode datos, String campo) {
        JsonNode valor = datos.get(campo);

        if (valor == null
                || !valor.isTextual()
                || valor.asText().isBlank()) {

            throw new IllegalStateException(
                    "Campo invalido en la respuesta de la API: " + campo
            );
        }

        return valor.asText();
    }

    /**
     * Lee un campo entero obligatorio del JSON.
     */
    private int obtenerEntero(JsonNode datos, String campo) {
        JsonNode valor = datos.get(campo);

        if (valor == null || !valor.isIntegralNumber()) {
            throw new IllegalStateException(
                    "Campo numerico invalido en la respuesta de la API: " + campo
            );
        }

        return valor.asInt();
    }
}
