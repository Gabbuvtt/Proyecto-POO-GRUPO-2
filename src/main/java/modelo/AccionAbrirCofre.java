package modelo;

import java.util.Locale;
import java.util.regex.Pattern;
import servicio.FalloAutenticacionException;
import servicio.PaqueteCofre;
import servicio.PaqueteCorruptoException;
import servicio.ServicioCriptografico;

/** Accion de abrir el cofre en X con la contrasena formada por las dos pistas. */
public class AccionAbrirCofre implements AccionJuego {

    private static final int MIN_LARGO = 4;
    private static final int MAX_LARGO = 20;
    private static final Pattern FORMATO = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)?");
    private static final int COSTO = 1;
    private static final String ID_ESPECIAL = "REL-ESPECIAL";
    private static final int VALOR_ESPECIAL = 4;

    private PaqueteCofre paquete;
    private String contrasena;
    private ServicioCriptografico servicio;

    /** Crea la accion con el paquete que el controlador pidio a la API y la contrasena escrita. */
    public AccionAbrirCofre(PaqueteCofre paquete, String contrasena) {
        this.paquete = paquete;
        this.contrasena = contrasena;
        this.servicio = new ServicioCriptografico();
    }

    /** Intenta abrir el cofre; solo cobra si se llega a descifrar. */
    @Override
    public ResultadoAccion ejecutar(Partida partida) {
        if (partida.getEstado() != EstadoDePartida.EN_CURSO) {
            return ResultadoAccion.PARTIDA_FINALIZADA;
        }

        Participante actual = partida.getTurnoActual();
        Casilla casilla = partida.getTablero().obtenerCasilla(actual.getPosicion());
        if (casilla.getTipo() != TipoDeTerreno.COFRE) {
            return ResultadoAccion.NO_ESTA_EN_COFRE;
        }

        // si ya esta abierto solo se informa, sin costo
        if (partida.estaCofreAbierto()) {
            return ResultadoAccion.COFRE_YA_ABIERTO;
        }
        if (!partida.tienenTodasLasPistas()) {
            return ResultadoAccion.FALTAN_PISTAS;
        }
        if (actual.getInventario().estaLleno()) {
            return ResultadoAccion.INVENTARIO_LLENO;
        }
        if (partida.getAccionesRestantes() < COSTO) {
            return ResultadoAccion.SIN_ACCIONES;
        }

        String texto = normalizar(contrasena);
        if (!formatoValido(texto)) {
            return ResultadoAccion.FORMATO_INVALIDO;
        }

        // el paquete debe ser de esta partida
        if (paquete == null || !partida.getPartidaId().equals(paquete.getPartidaId())) {
            return ResultadoAccion.DATOS_CORRUPTOS;
        }

        String json;
        try {
            json = servicio.descifrar(paquete, texto);
        } catch (FalloAutenticacionException e) {
            partida.consumirAcciones(COSTO);
            return ResultadoAccion.FALLO_AUTENTICACION;
        } catch (PaqueteCorruptoException e) {
            return ResultadoAccion.DATOS_CORRUPTOS;
        }

        // los ids del contenido deben coincidir con los del paquete
        if (!tieneCampo(json, "partidaId", paquete.getPartidaId())
                || !tieneCampo(json, "cofreId", paquete.getCofreId())) {
            return ResultadoAccion.DATOS_CORRUPTOS;
        }

        partida.abrirCofre();
        actual.getInventario().agregarReliquia(new Reliquia(ID_ESPECIAL, VALOR_ESPECIAL));
        partida.consumirAcciones(COSTO);
        return ResultadoAccion.OK;
    }

    /** Pasa la contrasena a mayusculas sin quitar espacios. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.toUpperCase(Locale.ROOT);
    }

    /** Indica si la contrasena ya normalizada tiene entre 4 y 20 caracteres y cumple el formato. */
    public static boolean formatoValido(String texto) {
        if (texto == null || texto.length() < MIN_LARGO || texto.length() > MAX_LARGO) {
            return false;
        }
        return FORMATO.matcher(texto).matches();
    }

    // busca "campo":"valor" en el JSON, ignorando espacios
    private boolean tieneCampo(String json, String campo, String valor) {
        String sinEspacios = json.replace(" ", "");
        return sinEspacios.contains("\"" + campo + "\":\"" + valor + "\"");
    }
}
