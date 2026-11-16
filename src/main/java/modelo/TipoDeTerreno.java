package modelo;

/** Tipos de terreno del tablero con su simbolo representativo y costo en acciones. */
public enum TipoDeTerreno {
    CAMPAMENTO('C', 1),
    SENDERO('S', 1),
    PANTANO('P', 2),
    TEMPLO('T', 1),
    COFRE('X', 1);

    private char simbolo;
    private int costo;

    TipoDeTerreno(char simbolo, int costo) {
        this.simbolo = simbolo;
        this.costo = costo;
    }

    /** Devuelve la letra que representa al terreno en el tablero. */
    public char getSimbolo() {
        return simbolo;
    }

    /** Devuelve la cantidad de acciones que cuesta entrar a este terreno. */
    public int getCosto() {
        return costo;
    }

    /** Busca y devuelve el tipo de terreno correspondiente a su caracter ('C', 'S', 'P', 'T', 'X'). */
    public static TipoDeTerreno buscarPorSimbolo(char letra) {
        if (letra == 'C' || letra == 'c') return CAMPAMENTO;
        else if (letra == 'S' || letra == 's') return SENDERO;
        else if (letra == 'P' || letra == 'p') return PANTANO;
        else if (letra == 'T' || letra == 't') return TEMPLO;
        else if (letra == 'X' || letra == 'x') return COFRE;
        return null;
    }
}