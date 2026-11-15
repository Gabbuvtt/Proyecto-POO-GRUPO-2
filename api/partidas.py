import random
import secrets
from enum import Enum

from cofre import cifrar_cofre, descifrar_cofre
from generador import (
    elegir_templos_pista,
    generar_fragmentos,
    generar_tablero,
    repartir_reliquias,
)
from tablero import TipoCasilla, a_coordenada, buscar_casillas
from validaciones import (
    campamento_tiene_vecinos,
    hay_templo_cercano,
    posicion_campamento,
    ruta_valida,
)

MAX_CANDIDATOS = 1000
COFRE_ID = "COFRE-01"
ACCIONES_POR_TURNO = 3


class EstadoPartida(str, Enum):
    EN_CURSO = "EN_CURSO"
    FINALIZADA = "FINALIZADA"


class GeneracionFallida(Exception):
    pass


def es_candidato_valido(tablero, templos_pista):
    return (
        campamento_tiene_vecinos(tablero)
        and hay_templo_cercano(tablero)
        and ruta_valida(tablero, templos_pista[0], templos_pista[1])
    )


def es_distinta(candidato, anterior):
    # C, X y los dos fragmentos tienen que cambiar respecto a la partida anterior
    if anterior is None:
        return True
    tablero = candidato["tablero"]
    campamento = coordenada(posicion_campamento(tablero))
    cofre = coordenada(buscar_casillas(tablero, TipoCasilla.COFRE)[0])
    pistas_anteriores = list(anterior["pistas"].values())
    return (
        campamento != anterior["campamento"]
        and cofre != anterior["cofre"]
        and candidato["pistas"][0] != pistas_anteriores[0]
        and candidato["pistas"][1] != pistas_anteriores[1]
    )


def armar_candidato(semilla, jugadores):
    # el mapa completo sale de la misma semilla para poder reproducirlo
    rng = random.Random(semilla)
    tablero = generar_tablero(semilla)
    templos = buscar_casillas(tablero, TipoCasilla.TEMPLO)
    reliquias = repartir_reliquias(templos, rng)
    templos_pista = elegir_templos_pista(templos, rng)
    pista1, pista2 = generar_fragmentos(rng)
    orden = list(jugadores)
    rng.shuffle(orden)
    return {
        "semilla": semilla,
        "tablero": tablero,
        "reliquias": reliquias,
        "templosPista": templos_pista,
        "pistas": [pista1, pista2],
        "orden": orden,
    }


def coordenada(posicion):
    return a_coordenada(posicion[0], posicion[1])


def a_partida(partida_id, jugadores, candidato, paquete):
    # Pasa las posiciones (fila, col) a coordenadas tipo C3
    tablero = candidato["tablero"]
    campamento = coordenada(posicion_campamento(tablero))
    cofre = coordenada(buscar_casillas(tablero, TipoCasilla.COFRE)[0])

    reliquias = {}
    for posicion, reliquia in candidato["reliquias"].items():
        reliquias[coordenada(posicion)] = reliquia

    pistas = {}
    for i in range(len(candidato["templosPista"])):
        pistas[coordenada(candidato["templosPista"][i])] = candidato["pistas"][i]

    posiciones = {}
    for jugador in jugadores:
        posiciones[jugador] = campamento

    return {
        "partidaId": partida_id,
        "semilla": candidato["semilla"],
        "tablero": tablero,
        "campamento": campamento,
        "cofre": cofre,
        "reliquias": reliquias,
        "pistas": pistas,
        "paquete": paquete,
        "jugadores": list(jugadores),
        "orden": candidato["orden"],
        "posiciones": posiciones,
        "estado": estado_inicial(candidato["orden"], campamento),
    }


def estado_inicial(orden, campamento):
    # ronda 1, le toca al primero del orden y todos parten de C
    jugadores = []
    for jugador in orden:
        jugadores.append({
            "jugadorId": jugador,
            "posicion": campamento,
            "puntos": 0,
            "inventario": [],
            "entregadas": [],
        })
    return {
        "estado": EstadoPartida.EN_CURSO.value,
        "ronda": 1,
        "indiceJugadorActual": 0,
        "accionesRestantes": ACCIONES_POR_TURNO,
        "jugadores": jugadores,
        "templosExplorados": [],
        "pistas": [],
        "cofreAbierto": False,
        "ganadores": [],
    }


def generar_partida(partida_id, jugadores, anterior=None):
    for _ in range(MAX_CANDIDATOS):
        semilla = secrets.randbits(32)
        candidato = armar_candidato(semilla, jugadores)
        if not es_candidato_valido(candidato["tablero"], candidato["templosPista"]):
            continue
        if not es_distinta(candidato, anterior):
            continue

        contrasena = candidato["pistas"][0] + "-" + candidato["pistas"][1]
        paquete = cifrar_cofre(partida_id, COFRE_ID, contrasena)
        # el paquete tiene que abrirse con su contrasena antes de aceptarlo
        try:
            descifrar_cofre(paquete, contrasena)
        except Exception:
            continue
        return a_partida(partida_id, jugadores, candidato, paquete)

    raise GeneracionFallida("No se pudo generar un tablero valido")
