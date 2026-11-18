package org.example;

import api.ClienteApi;
import api.RepositorioPartidas;
import configuracion.Configuracion;
import controlador.Controlador;
import servicio.ServicioCriptografico;
import vista.VistaConsola;

/** Arranca el juego: arma las piezas y abre el menu principal. */
public class Main {

    /** Punto de entrada del programa. */
    public static void main(String[] args) {
        VistaConsola vista = new VistaConsola();
        RepositorioPartidas repositorio;

        // si falta config.properties o la URL esta mal, se avisa sin caerse
        try {
            repositorio = new RepositorioPartidas(
                    new ClienteApi(Configuracion.getUrlBase())
            );
        } catch (RuntimeException | ExceptionInInitializerError e) {
            vista.mostrarError("No se pudo leer la configuracion de la API.");
            return;
        }

        Controlador controlador = new Controlador(
                vista, new ServicioCriptografico(), repositorio
        );

        try {
            controlador.iniciar();
        } catch (IllegalStateException e) {
            // se cerro la entrada del teclado
            vista.mostrarMensaje("Se cerro la entrada. Hasta pronto.");
        }
    }
}
