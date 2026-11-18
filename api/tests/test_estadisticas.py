import copy
import os
import tempfile
import unittest

from fastapi.testclient import TestClient

import almacen
import main


class PruebasEstadisticasCA10(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        almacen.usar_archivo(os.path.join(self.temp_dir.name, "expedicion.json"))
        self.cliente = TestClient(main.app)
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Luis Gomez", "alias": "luis_ucab"})

    def tearDown(self):
        self.temp_dir.cleanup()

    def _crear_partida(self, request_id):
        return self.cliente.post(
            "/api/v1/partidas",
            json={"requestId": request_id, "jugadores": ["J1", "J2"]},
        ).json()

    def test_estadisticas_ganador_empate_y_reintento_sin_duplicar(self):
        p1 = self._crear_partida("req-1")
        estado1 = copy.deepcopy(p1["estado"])
        estado1["estado"] = "FINALIZADA"
        estado1["ronda"] = 8
        estado1["accionesRestantes"] = 0
        estado1["ganadores"] = ["J1"]
        for jugador in estado1["jugadores"]:
            if jugador["jugadorId"] == "J1":
                jugador["puntos"] = 8
                jugador["entregadas"] = [
                    {"reliquiaId": "REL-01", "valor": 3},
                    {"reliquiaId": "REL-ESPECIAL", "valor": 4},
                    {"reliquiaId": "REL-04", "valor": 1},
                ]
            else:
                jugador["puntos"] = 4
                jugador["entregadas"] = [
                    {"reliquiaId": "REL-02", "valor": 2},
                    {"reliquiaId": "REL-03", "valor": 2},
                ]

        self.cliente.put(
            f"/api/v1/partidas/{p1['partidaId']}/estado",
            json={"revisionBase": 1, "estado": estado1},
        )
        # repetir guardado final no debe duplicar estadisticas
        self.cliente.put(
            f"/api/v1/partidas/{p1['partidaId']}/estado",
            json={"revisionBase": 1, "estado": estado1},
        )

        # segunda partida empatada: ambos suman victoria
        p2 = self._crear_partida("req-2")
        estado2 = copy.deepcopy(p2["estado"])
        estado2["estado"] = "FINALIZADA"
        estado2["ronda"] = 8
        estado2["accionesRestantes"] = 0
        estado2["ganadores"] = ["J1", "J2"]
        for jugador in estado2["jugadores"]:
            jugador["puntos"] = 2
            jugador["entregadas"] = [{"reliquiaId": "REL-02", "valor": 2}]
        self.cliente.put(
            f"/api/v1/partidas/{p2['partidaId']}/estado",
            json={"revisionBase": 1, "estado": estado2},
        )

        # tercera partida EN_CURSO no debe contar
        self._crear_partida("req-3")

        est_j1 = self.cliente.get("/api/v1/jugadores/J1/estadisticas")
        self.assertEqual(est_j1.status_code, 200)
        self.assertEqual(
            est_j1.json(),
            {
                "jugadorId": "J1",
                "partidasFinalizadas": 2,
                "victorias": 2,
                "puntosAcumulados": 10,
                "reliquiasEntregadas": 4,
            },
        )

        est_j2 = self.cliente.get("/api/v1/jugadores/J2/estadisticas")
        self.assertEqual(est_j2.status_code, 200)
        self.assertEqual(
            est_j2.json(),
            {
                "jugadorId": "J2",
                "partidasFinalizadas": 2,
                "victorias": 1,
                "puntosAcumulados": 6,
                "reliquiasEntregadas": 3,
            },
        )

    def test_jugador_inexistente_devuelve_404(self):
        respuesta = self.cliente.get("/api/v1/jugadores/J99/estadisticas")
        self.assertEqual(respuesta.status_code, 404)

    def test_repetir_creacion_con_mismo_request_id_deja_una_sola_partida(self):
        self._crear_partida("req-unico")
        self._crear_partida("req-unico")
        lista = self.cliente.get("/api/v1/partidas")
        self.assertEqual(len(lista.json()), 1)


if __name__ == "__main__":
    unittest.main()
