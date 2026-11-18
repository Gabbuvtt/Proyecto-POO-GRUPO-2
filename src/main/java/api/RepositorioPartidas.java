
package api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import controlador.AlmacenPartida;
import modelo.EstadoDePartida;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.Reliquia;
import servicio.PaqueteCofre;

import java.util.HashMap;
import java.util.Map;

/**
 * Repositorio encargado de comunicarse con la API Python
 * para guardar partidas y consultar el cofre.
 *
 * Implementa AlmacenPartida para mantener separado
 * el controlador de los detalles de HTTP y JSON.
 */
public class RepositorioPartidas implements AlmacenPartida {

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
                return confirmarPorConsulta(
                        partidaId, revisionBase, estado
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
                    "Campo de cofre invalido: " + campo
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
                    "Campo numerico de cofre invalido: " + campo
            );
        }

        return valor.asInt();
    }
}
