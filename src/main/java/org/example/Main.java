package org.example;

import modelo.*;

public class Main {
    public static void main(String[] args) {
        // 1. Crear el tablero 5x5
        Tablero tablero = new Tablero(5, 5);
        Coordenada campamento = new Coordenada(2, 2); // C3
        Coordenada templo1 = new Coordenada(1, 2);    // C2
        Coordenada pantano1 = new Coordenada(2, 3);   // D3
        Coordenada cofre = new Coordenada(0, 2);      // C1

        tablero.colocarTerreno(campamento, TipoDeTerreno.CAMPAMENTO);
        tablero.colocarTerreno(templo1, TipoDeTerreno.TEMPLO);
        tablero.colocarTerreno(pantano1, TipoDeTerreno.PANTANO);
        tablero.colocarTerreno(cofre, TipoDeTerreno.COFRE);

        // 2. Crear jugadores y participantes en el campamento (C3)
        Jugador j1 = new Jugador("J1", "Carlos", "Carlitos");
        Jugador j2 = new Jugador("J2", "Ana", "Anita");
        Participante p1 = new Participante(j1, campamento);
        Participante p2 = new Participante(j2, campamento);

        // 3. Crear la partida y esconder una reliquia y una pista en el templo C2
        Partida partida = new Partida("P-01", tablero, p1, p2);
        partida.esconderReliquia(templo1, new Reliquia("Ídolo de Oro", 3)); // Ajusta según el constructor de Reliquia
        partida.esconderPista(templo1, new Pista(1, "El cofre está al norte en C1")); // Usando int como primer parámetro

        System.out.println("=== INICIO DE LA PRUEBA DEL MODELO ===");
        System.out.println("Ronda: " + partida.getRondaActual() + " | Turno de: " + partida.getTurnoActual().getJugador().getNombre());
        System.out.println("Posición inicial: " + p1.getPosicion().getTexto() + " | Acciones: " + partida.getAccionesRestantes());

        // Accion 1 de Carlos: Moverse del Campamento (C3) al Templo (C2) -> Cuesta 1 acción
        ResultadoAccion mov1 = partida.ejecutarAccion(new AccionMover(templo1));
        System.out.println("\n1. Carlos se mueve a C2 (Templo): " + mov1 + " | Acciones restantes: " + partida.getAccionesRestantes());

        // Accion 2 de Carlos: Explorar el Templo en C2 -> Cuesta 1 acción
        ResultadoAccion exp1 = partida.ejecutarAccion(new AccionExplorar());
        System.out.println("2. Carlos explora C2: " + exp1 + " | Reliquias en mochila: " + p1.getInventario().getReliquias().size() + " | Pistas descubiertas: " + partida.getPistasDescubiertas().size());
        System.out.println("   Acciones restantes: " + partida.getAccionesRestantes());

        // Accion 3 de Carlos: Regresar al Campamento (C3) -> Cuesta 1 acción (Se queda en 0 acciones y cambia el turno a Ana)
        ResultadoAccion mov2 = partida.ejecutarAccion(new AccionMover(campamento));
        System.out.println("3. Carlos vuelve al Campamento C3: " + mov2);
        System.out.println("-> ¡Cambio automático de turno! Ahora juega: " + partida.getTurnoActual().getJugador().getNombre() + " con " + partida.getAccionesRestantes() + " acciones.");

        // Turno de Ana: Se mueve al Pantano (D3) que cuesta 2 acciones y luego regresa a C3 (1 acción)
        partida.ejecutarAccion(new AccionMover(pantano1));
        System.out.println("\n4. Ana entra al Pantano D3 (cuesta 2) | Acciones restantes de Ana: " + partida.getAccionesRestantes());
        partida.ejecutarAccion(new AccionMover(campamento));
        System.out.println("5. Ana vuelve a C3 (cuesta 1) -> Fin de la Ronda 1.");

        // Ronda 2 - Turno de Carlos: Entrega la reliquia en el Campamento
        System.out.println("\n=== RONDA " + partida.getRondaActual() + " | Turno de: " + partida.getTurnoActual().getJugador().getNombre() + " ===");
        ResultadoAccion ent1 = partida.ejecutarAccion(new AccionEntregar());
        System.out.println("6. Carlos entrega reliquias en el Campamento: " + ent1 + " | Puntos de Carlos: " + p1.getPuntos());
        System.out.println("Resultado parcial -> " + partida.obtenerGanador());
    }
}