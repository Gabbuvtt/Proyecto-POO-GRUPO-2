package modelo;

/** Representa una posicion en el tablero mediante fila, columna y texto (ej. "C3"). */
public class Coordenada {
    private int fila;
    private int columna;
    private String texto;

    /** Crea una coordenada a partir de su texto (ej. "C3" o "A10"), validando el formato. */
    public Coordenada(String texto) {
        if (texto == null || texto.trim().length() < 2) {
            this.texto = (texto == null) ? "" : texto.trim().toUpperCase();
            this.fila = -1;
            this.columna = -1;
            return;
        }

        this.texto = texto.trim().toUpperCase();
        char letra = this.texto.charAt(0);
        String parteNumero = this.texto.substring(1);

        if (letra >= 'A' && letra <= 'Z') {
            this.columna = letra - 'A';
        } else {
            this.columna = -1;
        }

        try {
            int numero = Integer.parseInt(parteNumero);
            this.fila = (numero >= 1) ? (numero - 1) : -1;
        } catch (NumberFormatException e) {
            this.fila = -1;
        }
    }

    /** Crea una coordenada a partir de sus indices numericos de fila y columna (base 0). */
    public Coordenada(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;

        if (fila >= 0 && columna >= 0 && columna < 26) {
            char letra = (char) ('A' + columna);
            this.texto = String.valueOf(letra) + (fila + 1);
        } else {
            this.texto = "";
        }
    }

    /** Devuelve el indice de la fila (base 0), o -1 si es invalida. */
    public int getFila() {
        return fila;
    }

    /** Devuelve el indice de la columna (base 0), o -1 si es invalida. */
    public int getColumna() {
        return columna;
    }

    /** Devuelve la representacion en texto de la coordenada (ej. "C3"). */
    public String getTexto() {
        return texto;
    }

    /** Indica si otra coordenada es vecina ortogonal (arriba, abajo, izquierda o derecha). */
    public boolean esVecinaOrtogonal(Coordenada cordenadaMoverse) {
        if (cordenadaMoverse == null) {
            return false;
        }

        int distFilas = Math.abs(this.fila - cordenadaMoverse.getFila());
        int distColumnas = Math.abs(this.columna - cordenadaMoverse.getColumna());

        return (distFilas + distColumnas) == 1;
    }

    /** Indica si dos coordenadas corresponden a la misma fila y columna. */
    public boolean esIgual(Coordenada cordenadaMoverse) {
        if (cordenadaMoverse == null) {
            return false;
        }
        return this.fila == cordenadaMoverse.getFila() && this.columna == cordenadaMoverse.getColumna();
    }
}