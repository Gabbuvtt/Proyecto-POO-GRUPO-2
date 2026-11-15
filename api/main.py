from fastapi import APIRouter, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse

from almacen import (
    buscar_partida,
    buscar_solicitud,
    guardar_partida,
    guardar_solicitud,
    jugador_existe,
    nuevo_partida_id,
    partida_anterior,
)
from modelos import ContenidoTemplo, Error, PaqueteCofre, PartidaPublica, PeticionPartida
from partidas import GeneracionFallida, generar_partida
from tablero import TipoCasilla, desde_coordenada

JUGADORES_POR_PARTIDA = 2


class ErrorApi(Exception):
    def __init__(self, estado_http, codigo, mensaje):
        self.estado_http = estado_http
        self.codigo = codigo
        self.mensaje = mensaje


app = FastAPI(title="Expedicion: La Isla de las Reliquias")
rutas = APIRouter(prefix="/api/v1")


@app.exception_handler(ErrorApi)
def manejar_error_api(request: Request, error: ErrorApi):
    return JSONResponse(
        status_code=error.estado_http,
        content={"codigo": error.codigo, "mensaje": error.mensaje},
    )


@app.exception_handler(RequestValidationError)
def manejar_cuerpo_invalido(request: Request, error: RequestValidationError):
    # pydantic devuelve 422, el enunciado pide 400
    return JSONResponse(
        status_code=400,
        content={"codigo": "DATOS_INVALIDOS", "mensaje": "El cuerpo de la peticion no es valido"},
    )


def validar_jugadores(jugadores):
    if len(jugadores) != JUGADORES_POR_PARTIDA or jugadores[0] == jugadores[1]:
        raise ErrorApi(400, "JUGADORES_INVALIDOS", "La partida necesita 2 jugadores distintos")
    for jugador in jugadores:
        if not jugador_existe(jugador):
            raise ErrorApi(404, "JUGADOR_NO_ENCONTRADO", "No existe el jugador " + jugador)


def a_publica(partida):
    # solo lo que el cliente puede ver: sin reliquias, pistas ni paquete
    terreno = []
    for fila in partida["tablero"]:
        letras = []
        for casilla in fila:
            letras.append(casilla.value)
        terreno.append(letras)
    return {
        "partidaId": partida["partidaId"],
        "terreno": terreno,
        "participantes": partida["jugadores"],
        "orden": partida["orden"],
        "posiciones": partida["posiciones"],
        "estado": partida["estado"],
    }


@rutas.post(
    "/partidas",
    status_code=201,
    response_model=PartidaPublica,
    summary="Crea una partida",
    description="Genera el tablero, las reliquias, las pistas y el cofre cifrado. "
                "Devuelve solo el terreno, las posiciones, los participantes, el orden y el estado inicial.",
    responses={
        400: {"model": Error, "description": "Jugadores repetidos o datos invalidos"},
        404: {"model": Error, "description": "Jugador inexistente"},
        409: {"model": Error, "description": "requestId ya usado con otro cuerpo"},
        503: {"model": Error, "description": "No se pudo generar un tablero valido"},
    },
)
def crear_partida(peticion: PeticionPartida):
    # mismo requestId: misma partida si el cuerpo es igual, 409 si cambia
    cuerpo = peticion.model_dump()
    solicitud = buscar_solicitud(peticion.requestId)
    if solicitud is not None:
        if solicitud["cuerpo"] != cuerpo:
            raise ErrorApi(409, "REQUEST_ID_CONFLICTIVO", "El requestId ya se uso con otro cuerpo")
        return a_publica(buscar_partida(solicitud["partidaId"]))

    validar_jugadores(peticion.jugadores)
    try:
        anterior = partida_anterior(peticion.jugadores)
        partida = generar_partida(nuevo_partida_id(), peticion.jugadores, anterior)
    except GeneracionFallida:
        raise ErrorApi(503, "GENERACION_FALLIDA", "No se pudo generar un tablero valido")
    guardar_partida(partida)
    guardar_solicitud(peticion.requestId, cuerpo, partida["partidaId"])
    return a_publica(partida)


def obtener_partida(partida_id):
    partida = buscar_partida(partida_id)
    if partida is None:
        raise ErrorApi(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida " + partida_id)
    return partida


def buscar_pista(partida, coord):
    # la pista 1 es el primer templo con pista y la 2 el segundo
    indice = 1
    for coord_pista, fragmento in partida["pistas"].items():
        if coord_pista == coord:
            return {"indice": indice, "fragmento": fragmento}
        indice += 1
    return None


@rutas.get(
    "/partidas/{partida_id}/templos/{coord}",
    response_model=ContenidoTemplo,
    summary="Consulta un templo",
    description="Devuelve la reliquia del templo (id y valor) y su pista si tiene. "
                "No gasta acciones ni modifica la partida.",
    responses={
        400: {"model": Error, "description": "Coordenada invalida o la casilla no es un templo"},
        404: {"model": Error, "description": "Partida inexistente"},
    },
)
def consultar_templo(partida_id: str, coord: str):
    partida = obtener_partida(partida_id)
    try:
        fila, col = desde_coordenada(coord)
    except ValueError:
        raise ErrorApi(400, "COORDENADA_INVALIDA", "La coordenada " + coord + " no es valida")
    if partida["tablero"][fila][col] != TipoCasilla.TEMPLO:
        raise ErrorApi(400, "NO_ES_TEMPLO", "La casilla " + coord + " no es un templo")
    return {
        "coordenada": coord,
        "reliquia": partida["reliquias"][coord],
        "pista": buscar_pista(partida, coord),
    }


@rutas.get(
    "/partidas/{partida_id}/cofre",
    response_model=PaqueteCofre,
    summary="Consulta el cofre",
    description="Devuelve el paquete cifrado completo del cofre (formato del Anexo B). "
                "Nunca incluye la contrasena, la clave ni el texto descifrado.",
    responses={
        400: {"model": Error, "description": "Peticion invalida"},
        404: {"model": Error, "description": "Partida inexistente"},
    },
)
def consultar_cofre(partida_id: str):
    partida = obtener_partida(partida_id)
    return partida["paquete"]


app.include_router(rutas)


def esquema_sin_422():
    # FastAPI documenta 422 solo, pero la API responde 400 en su lugar
    if app.openapi_schema is None:
        esquema = get_openapi(title=app.title, version=app.version, routes=app.routes)
        for ruta in esquema["paths"].values():
            for operacion in ruta.values():
                operacion["responses"].pop("422", None)
        esquema["components"]["schemas"].pop("HTTPValidationError", None)
        esquema["components"]["schemas"].pop("ValidationError", None)
        app.openapi_schema = esquema
    return app.openapi_schema


app.openapi = esquema_sin_422
