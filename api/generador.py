import random

from tablero import CANTIDADES, COLUMNAS, FILAS


def generar_tablero(semilla):
    # matriz FILAS x COLUMNAS con la cantidad exacta de cada tipo
    rng = random.Random(semilla)
    tipos = []
    for tipo, cantidad in CANTIDADES.items():
        for _ in range(cantidad):
            tipos.append(tipo)
    rng.shuffle(tipos)

    tablero = []
    for fila in range(FILAS):
        inicio = fila * COLUMNAS
        tablero.append(tipos[inicio:inicio + COLUMNAS])
    return tablero
