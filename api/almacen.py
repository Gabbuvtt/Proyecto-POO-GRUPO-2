import copy
import json
import os

from tablero import TipoCasilla

RUTA_DATOS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "datos", "expedicion.json")
REVISION_INICIAL = 1

_ruta_actual = RUTA_DATOS

jugadores = {}
partidas = {}
solicitudes = {}


class ErrorAlmacenamiento(Exception):
    pass


def _tablero_a_letras(tablero):
    resultado = []
    for fila in tablero:
        letras = []
        for casilla in fila:
            if isinstance(casilla, TipoCasilla):
                letras.append(casilla.value)
            else:
                letras.append(casilla)
        resultado.append(letras)
    return resultado


def _letras_a_tablero(letras):
    tablero = []
    for fila in letras:
        casillas = []
        for letra in fila:
            casillas.append(TipoCasilla(letra))
        tablero.append(casillas)
    return tablero


def _serializar_partidas():
    datos = {}
    for partida_id, partida in partidas.items():
        copia = copy.deepcopy(partida)
        copia["tablero"] = _tablero_a_letras(partida["tablero"])
        datos[partida_id] = copia
    return datos


def _cargar():
    jugadores.clear()
    partidas.clear()
    solicitudes.clear()
    if not os.path.exists(_ruta_actual):
        return
    try:
        with open(_ruta_actual, "r", encoding="utf-8") as archivo:
            contenido = json.load(archivo)
    except (OSError, ValueError) as error:
        raise ErrorAlmacenamiento("No se pudo leer el archivo de datos") from error

    for jugador_id, jugador in contenido.get("jugadores", {}).items():
        jugadores[jugador_id] = jugador
    for partida_id, partida in contenido.get("partidas", {}).items():
        partida["tablero"] = _letras_a_tablero(partida["tablero"])
        if "revision" not in partida:
            partida["revision"] = REVISION_INICIAL
        if "ultimoGuardado" not in partida:
            partida["ultimoGuardado"] = None
        partidas[partida_id] = partida
    for request_id, solicitud in contenido.get("solicitudes", {}).items():
        solicitudes[request_id] = solicitud


def _guardar_todo():
    carpeta = os.path.dirname(_ruta_actual)
    ruta_tmp = _ruta_actual + ".tmp"
    datos = {
        "jugadores": jugadores,
        "partidas": _serializar_partidas(),
        "solicitudes": solicitudes,
    }
    try:
        if carpeta:
            os.makedirs(carpeta, exist_ok=True)
        # escritura atomica: primero al temporal, luego reemplazo
        with open(ruta_tmp, "w", encoding="utf-8") as archivo:
            json.dump(datos, archivo, ensure_ascii=False, indent=2)
            archivo.flush()
            os.fsync(archivo.fileno())
        os.replace(ruta_tmp, _ruta_actual)
    except OSError as error:
        if os.path.exists(ruta_tmp):
            try:
                os.remove(ruta_tmp)
            except OSError:
                pass
        raise ErrorAlmacenamiento("No se pudo guardar el archivo de datos") from error


def usar_archivo(ruta):
    global _ruta_actual
    _ruta_actual = ruta
    _cargar()


def guardar_partida(partida):
    partida_id = partida["partidaId"]
    habia = partida_id in partidas
    anterior = partidas.get(partida_id)
    if "revision" not in partida:
        partida["revision"] = REVISION_INICIAL
    if "ultimoGuardado" not in partida:
        partida["ultimoGuardado"] = None
    partidas[partida_id] = partida
    try:
        _guardar_todo()
    except ErrorAlmacenamiento:
        if habia:
            partidas[partida_id] = anterior
        else:
            partidas.pop(partida_id, None)
        raise


def guardar_solicitud(request_id, cuerpo, partida_id):
    habia = request_id in solicitudes
    anterior = solicitudes.get(request_id)
    solicitudes[request_id] = {"cuerpo": cuerpo, "partidaId": partida_id}
    try:
        _guardar_todo()
    except ErrorAlmacenamiento:
        if habia:
            solicitudes[request_id] = anterior
        else:
            solicitudes.pop(request_id, None)
        raise


def buscar_solicitud(request_id):
    return solicitudes.get(request_id)


def buscar_partida(partida_id):
    return partidas.get(partida_id)


def partida_anterior(jugadores_par):
    # la ultima partida del mismo par, sin importar el orden
    anterior = None
    for partida in partidas.values():
        if sorted(partida["jugadores"]) == sorted(jugadores_par):
            anterior = partida
    return anterior


def contar_partidas():
    return len(partidas)


def nuevo_partida_id():
    # EX-001, EX-002, .....
    return "EX-" + str(contar_partidas() + 1).zfill(3)


def jugador_existe(jugador_id):
    return jugador_id in jugadores


def nuevo_jugador_id():
    return "J" + str(len(jugadores) + 1)


def registrar_jugador(nombre, alias):
    jugador_id = nuevo_jugador_id()
    jugador = {
        "jugadorId": jugador_id,
        "nombre": nombre,
        "alias": alias,
    }
    jugadores[jugador_id] = jugador
    try:
        _guardar_todo()
    except ErrorAlmacenamiento:
        jugadores.pop(jugador_id, None)
        raise
    return jugador


def buscar_jugador(jugador_id):
    return jugadores.get(jugador_id)


def buscar_jugador_por_alias(alias):
    alias_min = alias.lower()
    for jugador in jugadores.values():
        if jugador["alias"].lower() == alias_min:
            return jugador
    return None


def listar_jugadores(alias=None):
    if alias is None:
        return list(jugadores.values())
    alias_min = alias.lower()
    resultado = []
    for jugador in jugadores.values():
        if jugador["alias"].lower() == alias_min:
            resultado.append(jugador)
    return resultado


def listar_partidas(jugador_id=None):
    if jugador_id is None:
        return list(partidas.values())
    resultado = []
    for partida in partidas.values():
        if jugador_id in partida["jugadores"]:
            resultado.append(partida)
    return resultado


def guardar_estado(partida_id, revision_base, estado_dict):
    partida = partidas[partida_id]
    estado_anterior = partida["estado"]
    posiciones_anteriores = partida["posiciones"]
    revision_anterior = partida["revision"]
    guardado_anterior = partida.get("ultimoGuardado")

    nuevas_posiciones = {}
    for estado_jugador in estado_dict["jugadores"]:
        nuevas_posiciones[estado_jugador["jugadorId"]] = estado_jugador["posicion"]

    nueva_revision = revision_anterior + 1
    confirmacion = {
        "partidaId": partida_id,
        "revision": nueva_revision,
        "guardado": True,
    }
    partida["estado"] = copy.deepcopy(estado_dict)
    partida["posiciones"] = nuevas_posiciones
    partida["revision"] = nueva_revision
    partida["ultimoGuardado"] = {
        "revisionBase": revision_base,
        "estado": copy.deepcopy(estado_dict),
        "confirmacion": confirmacion,
    }
    try:
        _guardar_todo()
    except ErrorAlmacenamiento:
        partida["estado"] = estado_anterior
        partida["posiciones"] = posiciones_anteriores
        partida["revision"] = revision_anterior
        partida["ultimoGuardado"] = guardado_anterior
        raise
    return confirmacion


_cargar()
