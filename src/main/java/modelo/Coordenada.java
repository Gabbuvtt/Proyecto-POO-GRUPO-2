package modelo;

public class Coordenada {
    private int fila;
    private int columna;
    private String texto;

    // Constructor 1: Recibe el texto como "c3" o "C3", lo pasa a mayúscula y calcula fila y columna
    public Coordenada(String texto) {
        this.texto = texto.toUpperCase();

        char letra = this.texto.charAt(0);  // Saca la letra (ej. 'C')
        char numero = this.texto.charAt(1); // Saca el número (ej. '3')

        // Convertimos la letra en columna (A=0, B=1, C=2, D=3, E=4)
        if (letra == 'A') this.columna = 0;
        else if (letra == 'B') this.columna = 1;
        else if (letra == 'C') this.columna = 2;
        else if (letra == 'D') this.columna = 3;
        else if (letra == 'E') this.columna = 4;
        else this.columna = -1; // Letra fuera del tablero 5x5

        // Convertimos el número en fila (1=0, 2=1, 3=2, 4=3, 5=4)
        if (numero == '1') this.fila = 0;
        else if (numero == '2') this.fila = 1;
        else if (numero == '3') this.fila = 2;
        else if (numero == '4') this.fila = 3;
        else if (numero == '5') this.fila = 4;
        else this.fila = -1; // Número fuera del tablero 5x5
    }

    // Constructor 2: Recibe fila y columna numérica y arma el texto (ej. "C3")
    public Coordenada(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;

        String letra = "";
        if (columna == 0) letra = "A";
        else if (columna == 1) letra = "B";
        else if (columna == 2) letra = "C";
        else if (columna == 3) letra = "D";
        else if (columna == 4) letra = "E";

        int numeroFila = fila + 1;
        this.texto = letra + numeroFila;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public String getTexto() {
        return texto;
    }

    // Revisa si otra casilla está justo al lado (arriba, abajo, izquierda o derecha)
    public boolean esVecinaOrtogonal(Coordenada cordenadaMoverse) {
        if (cordenadaMoverse == null) {
            return false;
        }

        int distFilas = this.fila - cordenadaMoverse.getFila();
        if (distFilas < 0) {
            distFilas = -distFilas;
        }

        int distColumnas = this.columna - cordenadaMoverse.getColumna();
        if (distColumnas < 0) {
            distColumnas = -distColumnas;
        }

        if (distFilas + distColumnas == 1) {
            return true;
        } else {
            return false;
        }
    }

    // Compara si dos coordenadas son la misma casilla
    public boolean esIgual(Coordenada cordenadaMoverse) {
        if (cordenadaMoverse == null) {
            return false;
        }
        return this.fila == cordenadaMoverse.getFila() && this.columna == cordenadaMoverse.getColumna();
    }
}