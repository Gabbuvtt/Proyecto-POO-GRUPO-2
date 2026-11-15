# guardado en memoria, se reemplaza cuando exista el guardado real
partidas = {}
#requestId - cuerpo de la peticion y partida que se creo con el
solicitudes = {}


def guardar_partida(partida):
    partidas[partida["partidaId"]] = partida


def guardar_solicitud(request_id, cuerpo, partida_id):
    solicitudes[request_id] = {"cuerpo": cuerpo, "partidaId": partida_id}


def buscar_solicitud(request_id):
    return solicitudes.get(request_id)


def buscar_partida(partida_id):
    return partidas.get(partida_id)


def partida_anterior(jugadores):
    # la ultima partida del mismo par, sin importar el orden
    anterior = None
    for partida in partidas.values():
        if sorted(partida["jugadores"]) == sorted(jugadores):
            anterior = partida
    return anterior


def contar_partidas():
    return len(partidas)


def nuevo_partida_id():
    # EX-001, EX-002, .....
    return "EX-" + str(contar_partidas() + 1).zfill(3)


def jugador_existe(jugador_id):
    # pendiente: conectar con el registro de jugadores (POSt-jugadores)
    return True
