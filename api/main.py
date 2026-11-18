import re

from fastapi import APIRouter, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse

from almacen import (
    ErrorAlmacenamiento,
    buscar_jugador,
    buscar_jugador_por_alias,
    buscar_partida,
    buscar_solicitud,
    guardar_estado,
    guardar_partida,
    guardar_solicitud,
    jugador_existe,
    listar_jugadores,
    listar_partidas,
    nuevo_partida_id,
    partida_anterior,
    registrar_jugador,
)
from modelos import (
    ConfirmacionGuardado,
    ContenidoTemplo,
    Error,
    Estadisticas,
    JugadorRegistrado,
    PaqueteCofre,
    PartidaGuardada,
    PartidaPublica,
    PeticionEstado,
    PeticionJugador,
    PeticionPartida,
    ResumenPartida,
    Salud,
)
from partidas import GeneracionFallida, generar_partida
from tablero import TipoCasilla, desde_coordenada

JUGADORES_POR_PARTIDA = 2
VERSION_CONTRATO = 2
PATRON_NOMBRE = re.compile(r"^.{1,80}$")
PATRON_ALIAS = re.compile(r"^[A-Za-z0-9_-]{3,20}$")
MAX_RONDAS = 8
MAX_ACCIONES = 3
ESTADOS_VALIDOS = ("EN_CURSO", "FINALIZADA")


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


@app.exception_handler(ErrorAlmacenamiento)
def manejar_error_almacenamiento(request: Request, error: ErrorAlmacenamiento):
    return JSONResponse(
        status_code=503,
        content={"codigo": "ALMACENAMIENTO_NO_DISPONIBLE", "mensaje": str(error)},
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


def _tablero_a_terreno(tablero):
    terreno = []
    for fila in tablero:
        letras = []
        for casilla in fila:
            letras.append(casilla.value)
        terreno.append(letras)
    return terreno


def a_publica(partida):
    # solo lo que el cliente puede ver: sin reliquias, pistas ni paquete
    return {
        "partidaId": partida["partidaId"],
        "terreno": _tablero_a_terreno(partida["tablero"]),
        "participantes": partida["jugadores"],
        "orden": partida["orden"],
        "posiciones": partida["posiciones"],
        "estado": partida["estado"],
    }


@rutas.get(
    "/salud",
    response_model=Salud,
    summary="Comprueba la salud del servicio",
    description="Indica si la API esta disponible y la version del contrato.",
    responses={
        400: {"model": Error, "description": "Peticion invalida"},
    },
)
def consultar_salud():
    return {"disponible": True, "versionContrato": VERSION_CONTRATO}


@rutas.post(
    "/jugadores",
    status_code=201,
    response_model=JugadorRegistrado,
    summary="Registra un jugador",
    description="Crea un nuevo jugador con nombre y alias unico (sin distinguir mayusculas).",
    responses={
        400: {"model": Error, "description": "Nombre o alias invalido"},
        409: {"model": Error, "description": "El alias ya esta registrado"},
        503: {"model": Error, "description": "No se pudo guardar en almacenamiento"},
    },
)
def crear_jugador(peticion: PeticionJugador):
    nombre = peticion.nombre.strip()
    if not PATRON_NOMBRE.match(nombre):
        raise ErrorApi(400, "NOMBRE_INVALIDO", "El nombre debe tener entre 1 y 80 caracteres")
    if not PATRON_ALIAS.match(peticion.alias):
        raise ErrorApi(
            400,
            "ALIAS_INVALIDO",
            "El alias debe tener entre 3 y 20 caracteres (letras, digitos, guion o guion bajo)",
        )
    if buscar_jugador_por_alias(peticion.alias) is not None:
        raise ErrorApi(409, "ALIAS_REPETIDO", "Ya existe un jugador con el alias " + peticion.alias)
    return registrar_jugador(nombre, peticion.alias)


@rutas.get(
    "/jugadores",
    response_model=list[JugadorRegistrado],
    summary="Lista jugadores",
    description="Devuelve los jugadores registrados, con filtro opcional por alias.",
    responses={
        400: {"model": Error, "description": "Parametro de consulta invalido"},
    },
)
def obtener_jugadores(alias: str | None = None):
    return listar_jugadores(alias)


@rutas.get(
    "/jugadores/{jugador_id}/estadisticas",
    response_model=Estadisticas,
    summary="Consulta estadisticas de un jugador",
    description="Calcula partidas finalizadas, victorias, puntos acumulados y reliquias entregadas.",
    responses={
        400: {"model": Error, "description": "Peticion invalida"},
        404: {"model": Error, "description": "Jugador inexistente"},
    },
)
def consultar_estadisticas(jugador_id: str):
    if buscar_jugador(jugador_id) is None:
        raise ErrorApi(404, "JUGADOR_NO_ENCONTRADO", "No existe el jugador " + jugador_id)

    finalizadas = 0
    victorias = 0
    puntos = 0
    reliquias = 0

    for partida in listar_partidas(jugador_id):
        estado = partida["estado"]
        if estado["estado"] != "FINALIZADA":
            continue
        finalizadas += 1
        if jugador_id in estado["ganadores"]:
            victorias += 1
        for estado_jugador in estado["jugadores"]:
            if estado_jugador["jugadorId"] == jugador_id:
                puntos += estado_jugador["puntos"]
                reliquias += len(estado_jugador["entregadas"])
                break

    return {
        "jugadorId": jugador_id,
        "partidasFinalizadas": finalizadas,
        "victorias": victorias,
        "puntosAcumulados": puntos,
        "reliquiasEntregadas": reliquias,
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


@rutas.get(
    "/partidas",
    response_model=list[ResumenPartida],
    summary="Lista partidas",
    description="Devuelve el resumen de las partidas registradas, con filtro opcional por jugadorId.",
    responses={
        400: {"model": Error, "description": "Parametro de consulta invalido"},
    },
)
def obtener_partidas(jugadorId: str | None = None):
    resumenes = []
    for partida in listar_partidas(jugadorId):
        jugadores_resumen = []
        for jugador_id in partida["jugadores"]:
            jugador = buscar_jugador(jugador_id)
            alias = jugador["alias"] if jugador is not None else jugador_id
            jugadores_resumen.append({"jugadorId": jugador_id, "alias": alias})
        resumenes.append({
            "partidaId": partida["partidaId"],
            "jugadores": jugadores_resumen,
            "estado": partida["estado"]["estado"],
            "ronda": partida["estado"]["ronda"],
            "revision": partida["revision"],
        })
    return resumenes


def obtener_partida(partida_id):
    partida = buscar_partida(partida_id)
    if partida is None:
        raise ErrorApi(404, "PARTIDA_NO_ENCONTRADA", "No existe la partida " + partida_id)
    return partida


@rutas.get(
    "/partidas/{partida_id}",
    response_model=PartidaGuardada,
    summary="Consulta una partida guardada",
    description="Devuelve la configuracion publica inmutable y el ultimo snapshot guardado con su revision.",
    responses={
        400: {"model": Error, "description": "Peticion invalida"},
        404: {"model": Error, "description": "Partida inexistente"},
    },
)
def consultar_partida(partida_id: str):
    partida = obtener_partida(partida_id)
    return {
        "partidaId": partida["partidaId"],
        "terreno": _tablero_a_terreno(partida["tablero"]),
        "participantes": partida["jugadores"],
        "orden": partida["orden"],
        "campamento": partida["campamento"],
        "cofre": partida["cofre"],
        "revision": partida["revision"],
        "estado": partida["estado"],
    }


def _validar_estado_partida(partida, estado):
    if estado["estado"] not in ESTADOS_VALIDOS:
        raise ErrorApi(400, "ESTADO_INVALIDO", "El estado de la partida no es valido")
    if estado["ronda"] < 1 or estado["ronda"] > MAX_RONDAS:
        raise ErrorApi(400, "ESTADO_INVALIDO", "La ronda debe estar entre 1 y 8")
    if estado["accionesRestantes"] < 0 or estado["accionesRestantes"] > MAX_ACCIONES:
        raise ErrorApi(400, "ESTADO_INVALIDO", "Las acciones restantes deben estar entre 0 y 3")
    if estado["indiceJugadorActual"] not in (0, 1):
        raise ErrorApi(400, "ESTADO_INVALIDO", "El indice de jugador actual debe ser 0 o 1")

    ids_estado = []
    for estado_jugador in estado["jugadores"]:
        ids_estado.append(estado_jugador["jugadorId"])
    if len(ids_estado) != JUGADORES_POR_PARTIDA or sorted(ids_estado) != sorted(partida["jugadores"]):
        raise ErrorApi(400, "ESTADO_INVALIDO", "Los jugadores del estado no coinciden con la partida")

    if estado["estado"] == "FINALIZADA":
        if estado["ronda"] != MAX_RONDAS or estado["accionesRestantes"] != 0:
            raise ErrorApi(
                400,
                "ESTADO_INVALIDO",
                "Una partida finalizada debe estar en ronda 8 con 0 acciones restantes",
            )


@rutas.put(
    "/partidas/{partida_id}/estado",
    response_model=ConfirmacionGuardado,
    summary="Guarda el estado de una partida",
    description="Actualiza el snapshot de la partida usando control optimista con revisionBase e idempotencia.",
    responses={
        400: {"model": Error, "description": "Estado o datos invalidos"},
        404: {"model": Error, "description": "Partida inexistente"},
        409: {"model": Error, "description": "Revision incompatible o partida finalizada"},
        503: {"model": Error, "description": "No se pudo guardar en almacenamiento"},
    },
)
def guardar_estado_partida(partida_id: str, peticion: PeticionEstado):
    partida = obtener_partida(partida_id)
    estado = peticion.estado.model_dump()
    _validar_estado_partida(partida, estado)

    # reintento identico, misma confirmacion
    ultimo = partida.get("ultimoGuardado")
    if (
        ultimo is not None
        and ultimo["revisionBase"] == peticion.revisionBase
        and ultimo["estado"] == estado
    ):
        return ultimo["confirmacion"]

    if peticion.revisionBase != partida["revision"]:
        raise ErrorApi(
            409,
            "REVISION_INCOMPATIBLE",
            "La revision base no coincide con la revision actual de la partida",
        )

    if partida["estado"]["estado"] == "FINALIZADA":
        raise ErrorApi(
            409,
            "PARTIDA_FINALIZADA",
            "La partida ya finalizo y no admite mas cambios",
        )

    return guardar_estado(partida_id, peticion.revisionBase, estado)


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
