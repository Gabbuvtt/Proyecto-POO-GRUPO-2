package modelo;

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

    public char getSimbolo() {
        return simbolo;
    }

    public int getCosto() {
        return costo;
    }

    // Busca el terreno según la letra ('C', 'S', 'P', 'T', 'X')
    public static TipoDeTerreno buscarPorSimbolo(char letra){
        if (letra == 'C' || letra == 'c') return CAMPAMENTO;
        else if (letra == 'S' || letra == 's') return SENDERO;
        else if (letra == 'P' || letra == 'p') return PANTANO;
        else if (letra == 'T' || letra == 't') return TEMPLO;
        else if (letra == 'X' || letra == 'x') return COFRE;
        return null;
    }
}