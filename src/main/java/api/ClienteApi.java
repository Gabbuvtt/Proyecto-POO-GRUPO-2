
package api;

import configuracion.Configuracion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente encargado de comunicarse con la API Python mediante HTTP.
 * Permite realizar peticiones GET, POST y PUT.
 */
public class ClienteApi {

    private static final Duration TIEMPO_CONEXION = Duration.ofSeconds(5);
    private static final Duration TIEMPO_PETICION = Duration.ofSeconds(10);

    private final HttpClient cliente;
    private final String urlBase;

    /**
     * Crea el cliente utilizando la direccion del archivo de configuracion.
     */
    public ClienteApi() {
        this(Configuracion.getUrlBase());
    }

    /**
     * Crea el cliente con una direccion base especifica.
     *
     * @param urlBase direccion base del servidor API
     */
    public ClienteApi(String urlBase) {
        if (urlBase == null || urlBase.isBlank()) {
            throw new IllegalArgumentException(
                    "La URL base de la API no puede estar vacia."
            );
        }

        String direccion = urlBase.trim();

        while (direccion.endsWith("/")) {
            direccion = direccion.substring(0, direccion.length() - 1);
        }

        URI base = URI.create(direccion);

        if (base.getHost() == null ||
                (!"http".equalsIgnoreCase(base.getScheme())
                        && !"https".equalsIgnoreCase(base.getScheme()))) {
            throw new IllegalArgumentException(
                    "La direccion de la API no es valida."
            );
        }

        this.urlBase = direccion;

        // con HTTP/2 uvicorn pierde el cuerpo de los POST y PUT
        this.cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIEMPO_CONEXION)
                .build();
    }

    /**
     * Realiza una peticion GET.
     *
     * @param ruta ruta del endpoint
     * @return respuesta JSON del servidor
     */
    public String get(String ruta) {
        return enviarPeticion("GET", ruta, null);
    }

    /**
     * Realiza una peticion POST con un cuerpo JSON.
     *
     * @param ruta ruta del endpoint
     * @param cuerpoJson datos que se envian al servidor
     * @return respuesta JSON del servidor
     */
    public String post(String ruta, String cuerpoJson) {
        return enviarPeticion("POST", ruta, cuerpoJson);
    }

    /**
     * Realiza una peticion PUT con un cuerpo JSON.
     *
     * @param ruta ruta del endpoint
     * @param cuerpoJson datos que se envian al servidor
     * @return respuesta JSON del servidor
     */
    public String put(String ruta, String cuerpoJson) {
        return enviarPeticion("PUT", ruta, cuerpoJson);
    }
    private String enviarPeticion(
            String metodo,
            String ruta,
            String cuerpoJson
    ) {
        URI destino = construirUri(ruta);

        HttpRequest.Builder solicitud = HttpRequest.newBuilder()
                .uri(destino)
                .timeout(TIEMPO_PETICION)
                .header("Accept", "application/json");

        if ("GET".equals(metodo)) {
            solicitud.GET();
        } else {
            if (cuerpoJson == null) {
                throw new IllegalArgumentException(
                        "El cuerpo JSON no puede ser null."
                );
            }

            solicitud.header("Content-Type", "application/json; charset=UTF-8");

            solicitud.method(
                    metodo,
                    HttpRequest.BodyPublishers.ofString(
                            cuerpoJson,
                            StandardCharsets.UTF_8
                    )
            );
        }

        try {
            HttpResponse<String> respuesta = cliente.send(
                    solicitud.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            int codigo = respuesta.statusCode();

            if (codigo < 200 || codigo >= 300) {
                throw new ErrorApi(codigo, respuesta.body());
            }

            return respuesta.body();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "La comunicacion con la API fue interrumpida.",
                    e
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo establecer comunicacion con la API.",
                    e
            );
        }
    }

    /**
     * Construye la direccion completa de un endpoint.
     */
    private URI construirUri(String ruta) {
        if (ruta == null || ruta.isBlank()
                || ruta.contains("://")
                || ruta.startsWith("//")) {
            throw new IllegalArgumentException(
                    "La ruta del endpoint no es valida."
            );
        }

        String rutaNormalizada = ruta.startsWith("/")
                ? ruta
                : "/" + ruta;

        return URI.create(urlBase + rutaNormalizada);
    }

    /**
     * Representa un error devuelto por la API mediante HTTP.
     */
    public static class ErrorApi extends RuntimeException {

        private final int codigoEstado;
        private final String respuesta;

        /** Crea el error con el codigo HTTP y el cuerpo que mando la API. */
        public ErrorApi(int codigoEstado, String respuesta) {
            super("La API respondio con el codigo HTTP " + codigoEstado + ".");

            this.codigoEstado = codigoEstado;
            this.respuesta = respuesta;
        }

        /** Devuelve el codigo HTTP de la respuesta. */
        public int getCodigoEstado() {
            return codigoEstado;
        }

        /** Devuelve el cuerpo de la respuesta, normalmente {"codigo","mensaje"}. */
        public String getRespuesta() {
            return respuesta;
        }
    }
}
