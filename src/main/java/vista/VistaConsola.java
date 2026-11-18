package vista;

import modelo.Casilla;
import modelo.Coordenada;
import modelo.Participante;
import modelo.Partida;
import modelo.Pista;
import modelo.ReglasDelJuego;
import modelo.Reliquia;
import modelo.ResultadoAccion;
import modelo.Tablero;

import java.util.ArrayList;
import java.util.Scanner;

/** Muestra por consola las pantallas del juego. Solo imprime y lee: no decide nada de las reglas. */
public class VistaConsola {

    private final Scanner entrada;
    private final ValidadorEntrada validador = new ValidadorEntrada();
    private final MensajesResultado mensajes = new MensajesResultado();

    /** Crea la vista leyendo del teclado. */
    public VistaConsola() {
        this(new Scanner(System.in));
    }

    /** Crea la vista leyendo de un Scanner dado (sirve para probarla sin teclado). */
    public VistaConsola(Scanner entrada) {
        this.entrada = entrada;
    }

    /** Imprime un mensaje normal seguido de un salto de linea. */
    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    }

    /** Imprime un mensaje de error para que el usuario sepa que algo no se pudo hacer. */
    public void mostrarError(String mensaje) {
        System.out.println("Error: " + mensaje);
    }
    /** Muestra al ugador el resultado de haber intentado una accion. */
    public void mostrarResultado(ResultadoAccion resultado) {
        mostrarMensaje(mensajes.describir(resultado));
    }

    /** Muestra un texto y devuelve la linea tal cual la escribio el usuario, sin recortar espacios. */
    public String leerLineaSinRecortar(String mensaje) {
        System.out.print(mensaje);
        if (!entrada.hasNextLine()) {
            throw new IllegalStateException("No hay mas datos de entrada.");
        }
        return entrada.nextLine();
    }

    /** Muestra un texto y devuelve la linea que escribio el usuario, sin espacios en los extremos. */
    public String leerTexto(String mensaje) {
        return leerLineaSinRecortar(mensaje).trim();
    }
    /**
     * Pide un numero entero entre minimo y maximo. Si el usuario escribe letras, un numero fuera de
     * rango o nada, avisa y vuelve a preguntar. Asi el programa no se cae por una entrada mala.
     */
    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                int numero = Integer.parseInt(texto);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // no era un numero: se muestra el aviso de abajo
            }
            mostrarError("escribe un numero entre " + minimo + " y " + maximo + ".");
        }
    }

    /** Muestra el menu principal y devuelve la opcion elegida (0 a 5). */
    public int pedirOpcionMenuPrincipal() {
        System.out.println();
        System.out.println("EXPEDICIÓN: LA ISLA DE LAS RELIQUIAS");
        System.out.println("1. Registrar jugador");
        System.out.println("2. Nueva partida");
        System.out.println("3. Cargar partida");
        System.out.println("4. Consultar estadísticas");
        System.out.println("5. Reglas y ayuda");
        System.out.println("0. Salir");
        return leerEntero("Opción: ", 0, 5);
    }

    /** Pide nombre y alias repitiendo cada pregunta hasta que cumplan las reglas del enunciado. */
    public DatosRegistro pedirDatosRegistro() {
        System.out.println();
        System.out.println("Registro");

        String nombre = leerTexto("Nombre: ");
        while (!validador.esNombreValido(nombre)) {
            mostrarError("el nombre debe tener entre 1 y 80 caracteres.");
            nombre = leerTexto("Nombre: ");
        }

        String alias = leerTexto("Alias: ");
        while (!validador.esAliasValido(alias)) {
            mostrarError("el alias debe tener de 3 a 20 caracteres: letras, numeros, guion o guion bajo.");
            alias = leerTexto("Alias: ");
        }

        return new DatosRegistro(nombre, alias);
    }

    /** Informa que el jugador fue creado y cual es su identificador. */
    public void mostrarJugadorCreado(String jugadorId) {
        System.out.println("Jugador creado: " + jugadorId);
    }

    /** Pide un alias y lo repite hasta que tenga un formato valido. */
    public String pedirAlias(String mensaje) {
        String alias = leerTexto(mensaje);
        while (!validador.esAliasValido(alias)) {
            mostrarError("el alias debe tener de 3 a 20 caracteres: letras, numeros, guion o guion bajo.");
            alias = leerTexto(mensaje);
        }
        return alias;
    }

    /** Muestra el id, el campamento, el orden sorteado y la ronda de una partida nueva. */
    public void mostrarPartidaCreada(Partida partida) {
        System.out.println();
        System.out.println("Partida: " + partida.getPartidaId()
                + " | Campamento: " + partida.getParticipante1().getPosicion().getTexto());
        System.out.println("Orden sorteado: " + partida.getParticipante1().getJugador().getNombre()
                + ", " + partida.getParticipante2().getJugador().getNombre()
                + " | Ronda: " + partida.getRondaActual() + " de " + ReglasDelJuego.MAX_RONDAS);
    }

    /** Muestra una lista numerada y devuelve el numero elegido, o 0 para volver. */
    public int elegirDeLista(String titulo, ArrayList<String> opciones) {
        System.out.println();
        System.out.println(titulo);
        for (int i = 0; i < opciones.size(); i++) {
            System.out.println((i + 1) + ". " + opciones.get(i));
        }
        System.out.println("0. Volver");
        return leerEntero("Opción: ", 0, opciones.size());
    }

    /** Muestra las estadisticas de un jugador. */
    public void mostrarEstadisticas(String nombre, int finalizadas, int victorias,
                                    int puntos, int reliquias) {
        System.out.println();
        System.out.println("ESTADÍSTICAS DE " + nombre.toUpperCase());
        System.out.println("Partidas finalizadas: " + finalizadas);
        System.out.println("Victorias: " + victorias);
        System.out.println("Puntos acumulados: " + puntos);
        System.out.println("Reliquias entregadas: " + reliquias);
    }

    /** Resume las reglas del juego. */
    public void mostrarAyuda() {
        System.out.println();
        System.out.println("REGLAS Y AYUDA");
        System.out.println("- Cada jugador tiene 3 puntos de acción por turno y la partida dura 8 rondas.");
        System.out.println("- Moverse a una casilla vecina cuesta 1 (al pantano P cuesta 2). Explorar un templo cuesta 1.");
        System.out.println("- Las reliquias se entregan en el campamento C y entregar cuesta 1. Solo se cargan 2 a la vez.");
        System.out.println("- Para abrir el cofre X se unen la pista 1, un guion y la pista 2. Un intento fallido cuesta 1.");
        System.out.println("- Gana quien sume más puntos. Si empatan, quien entregó más reliquias.");
    }

    /** Pregunta si se quiere descifrar el vector del Anexo B. Devuelve 1 o 0. */
    public int pedirOpcionAyuda() {
        System.out.println();
        System.out.println("1. Descifrar el vector de prueba del Anexo B");
        System.out.println("0. Volver");
        return leerEntero("Opción: ", 0, 1);
    }

    /** Muestra el resultado de descifrar el vector del Anexo B. */
    public void mostrarVectorAnexoB(String partidaId, String cofreId, String contrasena, String textoClaro) {
        System.out.println();
        System.out.println("VECTOR DEL ANEXO B");
        System.out.println("Partida: " + partidaId + " | Cofre: " + cofreId);
        System.out.println("Contraseña: " + contrasena);
        System.out.println("Texto descifrado: " + textoClaro);
    }

    /** Muestra la cabecera del turno: partida, ronda, jugador, posicion, puntos, acciones e inventario. */
    public void mostrarEstadoTurno(Partida partida) {
        Participante actual = partida.getTurnoActual();
        System.out.println();
        System.out.println(partida.getPartidaId() + " | Ronda " + partida.getRondaActual()
                + " de " + ReglasDelJuego.MAX_RONDAS);
        System.out.println(actual.getJugador().getNombre() + " | Posición: " + actual.getPosicion().getTexto()
                + " | Puntos: " + actual.getPuntos() + " | Acciones: " + partida.getAccionesRestantes());
        System.out.println("Inventario: " + describirInventario(actual));
    }

    /** Muestra el menu del turno y devuelve la opcion elegida (0 a 7). */
    public int pedirOpcionTurno() {
        System.out.println();
        System.out.println("1. Mover");
        System.out.println("2. Explorar templo");
        System.out.println("3. Entregar reliquias");
        System.out.println("4. Abrir cofre");
        System.out.println("5. Consultar tablero, inventario y pistas");
        System.out.println("6. Guardar");
        System.out.println("7. Terminar turno");
        System.out.println("0. Guardar y salir");
        return leerEntero("Opción: ", 0, 7);
    }

    /** Pide la casilla destino. Puede ser invalida: el modelo la rechaza con FUERA_DEL_MAPA. */
    public Coordenada pedirCoordenadaDestino() {
        return new Coordenada(leerTexto("Destino: "));
    }

    /** Pantalla de consulta (no cuesta acciones): tablero, inventario del turno y pistas compartidas. */
    public void mostrarConsulta(Partida partida) {
        Participante p1 = partida.getParticipante1();
        Participante p2 = partida.getParticipante2();
        System.out.println();
        mostrarTablero(partida.getTablero(), p1, p2);
        System.out.println("1 = " + p1.getJugador().getNombre() + " | 2 = " + p2.getJugador().getNombre());
        System.out.println("C campamento, S sendero, P pantano, T templo, X cofre");
        System.out.println("Inventario: " + describirInventario(partida.getTurnoActual()));
        mostrarPistas(partida);
    }

    /** Muestra los fragmentos de pista conocidos sin unirlos, porque la contrasena la arma el jugador. */
    public void mostrarPistas(Partida partida) {
        System.out.println();
        System.out.println("PISTAS COMPARTIDAS");
        System.out.println("Pista 1: " + fragmentoDePista(partida, 1) + " | Pista 2: " + fragmentoDePista(partida, 2));
        System.out.println("Regla: unir pista 1, guion y pista 2.");
    }

    /** Devuelve el fragmento de la pista con ese indice, o "(sin descubrir)" si aun no se conoce. */
    private String fragmentoDePista(Partida partida, int indice) {
        for (Pista pista : partida.getPistasDescubiertas()) {
            if (pista.getIndice() == indice) {
                return pista.getFragmento();
            }
        }
        return "(sin descubrir)";
    }

    /** Describe el inventario de un participante, por ejemplo "REL-01, valor 3", o "vacío". */
    private String describirInventario(Participante participante) {
        ArrayList<Reliquia> reliquias = participante.getInventario().getReliquias();
        if (reliquias.isEmpty()) {
            return "vacío";
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < reliquias.size(); i++) {
            if (i > 0) {
                texto.append("; ");
            }
            texto.append(reliquias.get(i).getReliquiaId()).append(", valor ").append(reliquias.get(i).getValor());
        }
        return texto.toString();
    }
    /** Cuenta que reliquia y que pista se encontraron al explorar. La pista puede ser null. */
    public void mostrarExploracion(Reliquia reliquia, Pista pista, int accionesRestantes) {
        if (reliquia != null) {
            System.out.println("Encontraste " + reliquia.getReliquiaId() + ", valor " + reliquia.getValor() + ".");
        }
        if (pista != null) {
            System.out.println("Pista " + pista.getIndice() + " descubierta: " + pista.getFragmento());
        } else {
            System.out.println("Este templo no tenía pista.");
        }
        System.out.println("Acciones restantes: " + accionesRestantes + ".");
    }

    /** Avisa que el destino no es vecino de la posicion actual, como en la pantalla del enunciado. */
    public void mostrarMovimientoInvalido(String origen, int accionesRestantes) {
        System.out.println("Movimiento inválido: no es vecino ortogonal de " + origen + ".");
        System.out.println("Acciones restantes: " + accionesRestantes + ".");
    }

    /** Muestra un intento de cofre que no abrio. El fallo de contraseña va en una sola linea. */
    public void mostrarResultadoCofre(ResultadoAccion resultado, int accionesRestantes) {
        if (resultado == ResultadoAccion.FALLO_AUTENTICACION) {
            System.out.println("No se pudo autenticar el archivo. Acciones: " + accionesRestantes + ".");
        } else if (resultado == ResultadoAccion.INVENTARIO_LLENO) {
            System.out.println("Regrese al campamento para entregar antes de abrir el cofre.");
            System.out.println("Acciones restantes: " + accionesRestantes + ".");
        } else {
            mostrarResultado(resultado, accionesRestantes);
        }
    }

    /** Muestra el resultado de una accion y cuantos puntos de accion le quedan al jugador. */
    public void mostrarResultado(ResultadoAccion resultado, int accionesRestantes) {
        mostrarResultado(resultado);
        System.out.println("Acciones restantes: " + accionesRestantes + ".");
    }

    /**
     * Pantalla del cofre: muestra las pistas y pide la contrasena tal cual la escribe el jugador
     */
    public String pedirContrasenaCofre(Partida partida) {
        mostrarPistas(partida);
        Participante actual = partida.getTurnoActual();
        String estadoCofre = partida.estaCofreAbierto() ? "Cofre abierto" : "Cofre cerrado";
        System.out.println();
        System.out.println(actual.getJugador().getNombre() + " en " + actual.getPosicion().getTexto()
                + " | " + estadoCofre + " | Acciones: " + partida.getAccionesRestantes());
        return leerLineaSinRecortar("Contraseña: ");
    }

    /** Informa que el cofre se abrio y que la reliquia especial todavia hay que entregarla. */
    public void mostrarCofreAbierto(int accionesRestantes) {
        System.out.println("Cofre abierto. REL-ESPECIAL incorporada al inventario.");
        System.out.println("Valor: " + ReglasDelJuego.VALOR_RELIQUIA_ESPECIAL + " | Acciones restantes: " + accionesRestantes);
        System.out.println("Recuerde entregarla en el campamento.");
    }

    /** Pantalla final: puntos, reliquias entregadas y tiempo de cada jugador, y quien gano. */
    public void mostrarFinDePartida(Partida partida) {
        System.out.println();
        System.out.println("FIN DE LA PARTIDA");
        System.out.println(resumenFinal(partida.getParticipante1()));
        System.out.println(resumenFinal(partida.getParticipante2()));
        ArrayList<Participante> ganadores = partida.obtenerGanadores();
        if (ganadores.size() == 1) {
            System.out.println("Ganador: " + ganadores.get(0).getJugador().getNombre());
        } else {
            System.out.println("Victoria compartida.");
        }
    }

    /** Una linea con el resultado final de un participante, incluido lo que le quedo en el inventario. */
    private String resumenFinal(Participante participante) {
        return participante.getJugador().getNombre() + ": " + participante.getPuntos() + " puntos, "
                + participante.getEntregadas().size() + " reliquias entregadas, "
                + participante.getSegundosConsumidos() + " s de juego"
                + " | Inventario final: " + describirInventario(participante);
    }

    /**
     * Dibuja el tablero con las letras de las columnas arriba y los numeros de fila a la izquierda.
     * Sobre cada casilla marca con 1 y 2 donde estan los participantes.
     */
    public void mostrarTablero(Tablero tablero, Participante p1, Participante p2) {
        StringBuilder texto = new StringBuilder();

        // Encabezado: A B C D E
        texto.append("     ");
        for (int c = 0; c < tablero.getColumnas(); c++) {
            texto.append((char) ('A' + c)).append("    ");
        }
        texto.append("\n");

        // Una linea por fila del tablero
        for (int f = 0; f < tablero.getFilas(); f++) {
            texto.append(" ").append(f + 1).append("   ");
            for (int c = 0; c < tablero.getColumnas(); c++) {
                Coordenada coord = new Coordenada(f, c);
                Casilla casilla = tablero.obtenerCasilla(coord);

                String celda = String.valueOf(casilla.getTipo().getSimbolo());
                if (p1.getPosicion().esIgual(coord)) {
                    celda = celda + "1";
                }
                if (p2.getPosicion().esIgual(coord)) {
                    celda = celda + "2";
                }
                texto.append(String.format("%-5s", celda));
            }
            texto.append("\n");
        }

        System.out.print(texto);
    }
}