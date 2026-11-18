import os
import tempfile
import unittest
from unittest import mock

from fastapi.testclient import TestClient

import almacen
import main
from main import crear_partida
from modelos import PeticionPartida
from tablero import TipoCasilla


class PruebasAlmacen(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.ruta = os.path.join(self.temp_dir.name, "datos", "expedicion.json")
        almacen.usar_archivo(self.ruta)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_persiste_y_recarga_jugadores_y_partidas_con_enums(self):
        almacen.registrar_jugador("Ana Perez", "ana_ucab")
        almacen.registrar_jugador("Luis Gomez", "luis_ucab")
        partida_pub = crear_partida(PeticionPartida(requestId="req-1", jugadores=["J1", "J2"]))
        partida_id = partida_pub["partidaId"]

        self.assertTrue(os.path.exists(self.ruta))
        self.assertFalse(os.path.exists(self.ruta + ".tmp"))

        almacen.usar_archivo(self.ruta)
        self.assertTrue(almacen.jugador_existe("J1"))
        self.assertTrue(almacen.jugador_existe("J2"))

        recargada = almacen.buscar_partida(partida_id)
        self.assertIsNotNone(recargada)
        for fila in recargada["tablero"]:
            for casilla in fila:
                self.assertIsInstance(casilla, TipoCasilla)

    def test_error_escritura_devuelve_503_y_no_ensucia_memoria(self):
        cliente = TestClient(main.app)
        with mock.patch("almacen.os.replace", side_effect=OSError("disco lleno")):
            respuesta = cliente.post(
                "/api/v1/jugadores",
                json={"nombre": "Ana Perez", "alias": "ana_ucab"},
            )
        self.assertEqual(respuesta.status_code, 503)
        self.assertEqual(respuesta.json()["codigo"], "ALMACENAMIENTO_NO_DISPONIBLE")
        self.assertEqual(almacen.jugadores, {})


if __name__ == "__main__":
    unittest.main()
