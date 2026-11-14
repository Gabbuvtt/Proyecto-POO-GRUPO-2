from collections import deque

from tablero import COLUMNAS, FILAS, TipoCasilla, vecinos


def distancias_desde(tablero, origen):
    # BFS sin pasar por pantanos; None en las casillas a las que no se llega
    distancias = []
    for _ in range(FILAS):
        distancias.append([None] * COLUMNAS)

    fila, col = origen
    distancias[fila][col] = 0
    cola = deque([origen])
    while cola:
        fila, col = cola.popleft()
        for vecina_fila, vecina_col in vecinos(fila, col):
            if tablero[vecina_fila][vecina_col] == TipoCasilla.PANTANO:
                continue
            if distancias[vecina_fila][vecina_col] is None:
                distancias[vecina_fila][vecina_col] = distancias[fila][col] + 1
                cola.append((vecina_fila, vecina_col))
    return distancias
