import os
import tempfile
import unittest

from fastapi.testclient import TestClient

import almacen
import main


class PruebasJugadoresCA01(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        almacen.usar_archivo(os.path.join(self.temp_dir.name, "expedicion.json"))
        self.cliente = TestClient(main.app)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_registrar_jugador_devuelve_201_y_j1(self):
        respuesta = self.cliente.post(
            "/api/v1/jugadores",
            json={"nombre": "  Ana Perez  ", "alias": "ana_ucab"},
        )
        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(
            respuesta.json(),
            {"jugadorId": "J1", "nombre": "Ana Perez", "alias": "ana_ucab"},
        )

    def test_nombre_vacio_o_muy_largo_devuelve_400(self):
        for nombre in ["", "   ", "A" * 81]:
            respuesta = self.cliente.post(
                "/api/v1/jugadores",
                json={"nombre": nombre, "alias": "ana_ucab"},
            )
            self.assertEqual(respuesta.status_code, 400)
            self.assertEqual(respuesta.json()["codigo"], "NOMBRE_INVALIDO")

    def test_alias_invalido_devuelve_400(self):
        for alias in ["ab", "A" * 21, "ana ucab", "ana@ucab", ""]:
            respuesta = self.cliente.post(
                "/api/v1/jugadores",
                json={"nombre": "Ana Perez", "alias": alias},
            )
            self.assertEqual(respuesta.status_code, 400)
            self.assertEqual(respuesta.json()["codigo"], "ALIAS_INVALIDO")

    def test_alias_repetido_sin_distinguir_mayusculas_devuelve_409(self):
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        respuesta = self.cliente.post(
            "/api/v1/jugadores",
            json={"nombre": "Otra Ana", "alias": "ANA_UCAB"},
        )
        self.assertEqual(respuesta.status_code, 409)
        self.assertEqual(respuesta.json()["codigo"], "ALIAS_REPETIDO")

    def test_listar_jugadores_y_filtrar_por_alias_sin_mayusculas(self):
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Luis Gomez", "alias": "luis_ucab"})

        todos = self.cliente.get("/api/v1/jugadores")
        self.assertEqual(todos.status_code, 200)
        self.assertEqual(len(todos.json()), 2)

        filtrado = self.cliente.get("/api/v1/jugadores?alias=ANA_UCAB")
        self.assertEqual(filtrado.status_code, 200)
        self.assertEqual(len(filtrado.json()), 1)
        self.assertEqual(filtrado.json()[0]["jugadorId"], "J1")

        vacio = self.cliente.get("/api/v1/jugadores?alias=no_existe")
        self.assertEqual(vacio.status_code, 200)
        self.assertEqual(vacio.json(), [])

    def test_crear_partida_con_mismo_jugador_dos_veces_devuelve_400(self):
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        respuesta = self.cliente.post(
            "/api/v1/partidas",
            json={"requestId": "req-1", "jugadores": ["J1", "J1"]},
        )
        self.assertEqual(respuesta.status_code, 400)

    def test_crear_partida_con_jugador_no_registrado_devuelve_404(self):
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        respuesta = self.cliente.post(
            "/api/v1/partidas",
            json={"requestId": "req-1", "jugadores": ["J1", "J99"]},
        )
        self.assertEqual(respuesta.status_code, 404)


if __name__ == "__main__":
    unittest.main()
