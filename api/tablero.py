from enum import Enum

FILAS = 5
COLUMNAS = 5
LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"


class TipoCasilla(Enum):
    CAMPAMENTO = "C"
    COFRE = "X"
    TEMPLO = "T"
    PANTANO = "P"
    SENDERO = "S"


# cantidad exacta de cada tipo en el tablero
CANTIDADES = {
    TipoCasilla.CAMPAMENTO: 1,
    TipoCasilla.COFRE: 1,
    TipoCasilla.TEMPLO: 4,
    TipoCasilla.PANTANO: 4,
    TipoCasilla.SENDERO: 15,
}


def dentro_del_tablero(fila, col):
    return 0 <= fila < FILAS and 0 <= col < COLUMNAS


def a_coordenada(fila, col):
    if not dentro_del_tablero(fila, col):
        raise ValueError("Casilla fuera del tablero")
    return LETRAS[col] + str(fila + 1)


def desde_coordenada(coordenada):
    # C3 -> (2, 2), la letra es la columna y el numero la fila
    if len(coordenada) < 2:
        raise ValueError("Coordenada invalida")
    letra = coordenada[0]
    numero = coordenada[1:]
    if letra not in LETRAS or not numero.isdigit():
        raise ValueError("Coordenada invalida")
    fila = int(numero) - 1
    col = LETRAS.index(letra)
    if not dentro_del_tablero(fila, col):
        raise ValueError("Coordenada fuera del tablero")
    return fila, col


def buscar_casillas(tablero, tipo):
    # devuelve las posiciones (fila, col) de un tipo de casilla
    posiciones = []
    for fila in range(FILAS):
        for col in range(COLUMNAS):
            if tablero[fila][col] == tipo:
                posiciones.append((fila, col))
    return posiciones


def vecinos(fila, col):
    # solo arriba, abajo, izquierda y derecha
    resultado = []
    for df, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        nueva_fila = fila + df
        nueva_col = col + dc
        if dentro_del_tablero(nueva_fila, nueva_col):
            resultado.append((nueva_fila, nueva_col))
    return resultado
