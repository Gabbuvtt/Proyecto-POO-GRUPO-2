from pydantic import BaseModel, Field


class Error(BaseModel):
    codigo: str
    mensaje: str


class PeticionPartida(BaseModel):
    requestId: str = Field(min_length=1)
    jugadores: list[str]


class Reliquia(BaseModel):
    reliquiaId: str
    valor: int


class Pista(BaseModel):
    indice: int
    fragmento: str


class EstadoJugador(BaseModel):
    jugadorId: str
    posicion: str
    puntos: int
    inventario: list[Reliquia]
    entregadas: list[Reliquia]


class EstadoPartida(BaseModel):
    estado: str
    ronda: int
    indiceJugadorActual: int
    accionesRestantes: int
    jugadores: list[EstadoJugador]
    templosExplorados: list[str]
    pistas: list[Pista]
    cofreAbierto: bool
    ganadores: list[str]


class ContenidoTemplo(BaseModel):
    coordenada: str
    reliquia: Reliquia
    pista: Pista | None


class PaqueteCofre(BaseModel):
    partidaId: str
    cofreId: str
    algoritmo: str
    kdf: str
    iteraciones: int
    longitudClaveBits: int
    saltBase64: str
    nonceBase64: str
    tagBits: int
    aadBase64: str
    cifradoConTagBase64: str


class PartidaPublica(BaseModel):
    partidaId: str
    terreno: list[list[str]]
    participantes: list[str]
    orden: list[str]
    posiciones: dict[str, str]
    estado: EstadoPartida
