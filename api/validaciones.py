from collections import deque

from tablero import COLUMNAS, FILAS, TipoCasilla, buscar_casillas, vecinos

MIN_VECINOS_CAMPAMENTO = 2
MAX_DISTANCIA_TEMPLO = 2
MAX_MOVIMIENTOS = 12


def distancias_desde(tablero, origen):
    # BFS sin pasar por pantanos, none en las casillas a las que no se llega
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


def posicion_campamento(tablero):
    return buscar_casillas(tablero, TipoCasilla.CAMPAMENTO)[0]


def campamento_tiene_vecinos(tablero):
    # al menos 2 vecinos de C que no sean pantano
    fila, col = posicion_campamento(tablero)
    libres = 0
    for vecina_fila, vecina_col in vecinos(fila, col):
        if tablero[vecina_fila][vecina_col] != TipoCasilla.PANTANO:
            libres += 1
    return libres >= MIN_VECINOS_CAMPAMENTO


def hay_templo_cercano(tablero):
    # algun templo a 2 movimientos o menos de C
    distancias = distancias_desde(tablero, posicion_campamento(tablero))
    for fila, col in buscar_casillas(tablero, TipoCasilla.TEMPLO):
        distancia = distancias[fila][col]
        if distancia is not None and distancia <= MAX_DISTANCIA_TEMPLO:
            return True
    return False


def largo_ruta(tablero, templo1, templo2):
    # Cuenta los pasos de C a los templos, de vuelta a C, al cofre y otra vez a C
    campamento = posicion_campamento(tablero)
    cofre = buscar_casillas(tablero, TipoCasilla.COFRE)[0]
    paradas = [campamento, templo1, templo2, campamento, cofre, campamento]
    total = 0
    for i in range(len(paradas) - 1):
        distancias = distancias_desde(tablero, paradas[i])
        fila, col = paradas[i + 1]
        tramo = distancias[fila][col]
        if tramo is None:
            return None
        total += tramo
    return total


def ruta_valida(tablero, templo1, templo2):
    # se permite visitar los templos en cualquier orden
    for primero, segundo in [(templo1, templo2), (templo2, templo1)]:
        largo = largo_ruta(tablero, primero, segundo)
        if largo is not None and largo <= MAX_MOVIMIENTOS:
            return True
    return False
