from fastapi import APIRouter, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from almacen import guardar_partida, jugador_existe, nuevo_partida_id, partida_anterior
from modelos import Error, PartidaPublica, PeticionPartida
from partidas import GeneracionFallida, generar_partida

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
        503: {"model": Error, "description": "No se pudo generar un tablero valido"},
    },
)
def crear_partida(peticion: PeticionPartida):
    validar_jugadores(peticion.jugadores)
    try:
        anterior = partida_anterior(peticion.jugadores)
        partida = generar_partida(nuevo_partida_id(), peticion.jugadores, anterior)
    except GeneracionFallida:
        raise ErrorApi(503, "GENERACION_FALLIDA", "No se pudo generar un tablero valido")
    guardar_partida(partida)
    return a_publica(partida)


app.include_router(rutas)
