import random
import string

from tablero import CANTIDADES, COLUMNAS, FILAS

VALORES_RELIQUIAS = [1, 2, 2, 3]
TEMPLOS_CON_PISTA = 2
LARGO_PISTA_1 = 6
LARGO_PISTA_2 = 4


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


def repartir_reliquias(templos, rng):
    # a cada templo le toca una reliquia REL-01...REL-04 con valor 1, 2, 2 o 3
    valores = list(VALORES_RELIQUIAS)
    rng.shuffle(valores)
    reliquias = {}
    for i in range(len(templos)):
        reliquia_id = "REL-0" + str(i + 1)
        reliquias[templos[i]] = {"reliquiaId": reliquia_id, "valor": valores[i]}
    return reliquias


def elegir_templos_pista(templos, rng):
    # el primero guarda la pista 1 y el segundo la pista 2
    return rng.sample(templos, TEMPLOS_CON_PISTA)


def generar_fragmentos(rng):
    # pista 1: 6 letras, pista 2: 4 digitos como texto para manteenr ceros
    pista1 = ""
    for _ in range(LARGO_PISTA_1):
        pista1 += rng.choice(string.ascii_uppercase)
    pista2 = ""
    for _ in range(LARGO_PISTA_2):
        pista2 += rng.choice(string.digits)
    return pista1, pista2
