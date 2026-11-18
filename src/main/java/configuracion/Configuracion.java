
package configuracion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Lee la configuracion general del proyecto desde config.properties.
 */
public final class Configuracion {

    private static final String ARCHIVO = "/config.properties";
    private static final Properties PROPIEDADES = cargarPropiedades();

    /**
     * Carga las propiedades del archivo de configuracion.
     */
    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();

        try (InputStream entrada =
                     Configuracion.class.getResourceAsStream(ARCHIVO)) {

            if (entrada == null) {
                throw new IllegalStateException(
                        "No se encontro el archivo config.properties."
                );
            }

            propiedades.load(entrada);
            return propiedades;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer el archivo de configuracion.", e
            );
        }
    }

    /** Devuelve la URL base de la API escrita en config.properties. */
    public static String getUrlBase() {
        String url = PROPIEDADES.getProperty("urlBase");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "La propiedad urlBase no esta configurada."
            );
        }

        return url.trim();
    }

    private Configuracion() {
    }
}
