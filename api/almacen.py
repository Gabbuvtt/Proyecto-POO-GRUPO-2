# guardado en memoria, se reemplaza cuando exista el guardado real
partidas = {}


def guardar_partida(partida):
    partidas[partida["partidaId"]] = partida


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
