import copy
import os
import tempfile
import unittest

from fastapi.testclient import TestClient

import almacen
import main


class PruebasEstadoCA09yCA10(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        almacen.usar_archivo(os.path.join(self.temp_dir.name, "expedicion.json"))
        self.cliente = TestClient(main.app)
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Ana Perez", "alias": "ana_ucab"})
        self.cliente.post("/api/v1/jugadores", json={"nombre": "Luis Gomez", "alias": "luis_ucab"})
        creada = self.cliente.post(
            "/api/v1/partidas",
            json={"requestId": "req-001", "jugadores": ["J1", "J2"]},
        ).json()
        self.partida_id = creada["partidaId"]
        self.estado_inicial = creada["estado"]

    def tearDown(self):
        self.temp_dir.cleanup()

    def _estado_mitad_turno(self):
        estado = copy.deepcopy(self.estado_inicial)
        estado["ronda"] = 2
        estado["accionesRestantes"] = 1
        estado["templosExplorados"] = ["B3"]
        estado["pistas"] = [{"indice": 1, "fragmento": "ORBITA"}]
        estado["jugadores"][0]["posicion"] = "B3"
        estado["jugadores"][0]["inventario"] = [{"reliquiaId": "REL-01", "valor": 3}]
        return estado

    def _estado_finalizado(self):
        estado = copy.deepcopy(self.estado_inicial)
        estado["estado"] = "FINALIZADA"
        estado["ronda"] = 8
        estado["accionesRestantes"] = 0
        estado["ganadores"] = [estado["jugadores"][0]["jugadorId"]]
        estado["jugadores"][0]["puntos"] = 5
        estado["jugadores"][0]["entregadas"] = [
            {"reliquiaId": "REL-01", "valor": 3},
            {"reliquiaId": "REL-02", "valor": 2},
        ]
        return estado

    def test_guardar_y_consultar_estado_a_mitad_de_turno(self):
        estado = self._estado_mitad_turno()
        guardado = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado},
        )
        self.assertEqual(guardado.status_code, 200)
        self.assertEqual(guardado.json(), {"partidaId": self.partida_id, "revision": 2, "guardado": True})

        consulta = self.cliente.get(f"/api/v1/partidas/{self.partida_id}")
        self.assertEqual(consulta.status_code, 200)
        cuerpo = consulta.json()
        self.assertEqual(cuerpo["revision"], 2)
        self.assertEqual(cuerpo["estado"], estado)
        for campo in ["reliquias", "paquete", "semilla"]:
            self.assertNotIn(campo, cuerpo)

    def test_listar_partidas_y_filtrar_por_jugador(self):
        lista = self.cliente.get("/api/v1/partidas?jugadorId=J1")
        self.assertEqual(lista.status_code, 200)
        self.assertEqual(len(lista.json()), 1)
        self.assertEqual(lista.json()[0]["partidaId"], self.partida_id)
        self.assertEqual(lista.json()[0]["revision"], 1)

        lista_vacia = self.cliente.get("/api/v1/partidas?jugadorId=J99")
        self.assertEqual(lista_vacia.status_code, 200)
        self.assertEqual(lista_vacia.json(), [])

    def test_reintento_identico_devuelve_misma_confirmacion_sin_subir_revision(self):
        estado = self._estado_mitad_turno()
        primero = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado},
        )
        segundo = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado},
        )
        self.assertEqual(segundo.status_code, 200)
        self.assertEqual(segundo.json(), primero.json())
        consulta = self.cliente.get(f"/api/v1/partidas/{self.partida_id}")
        self.assertEqual(consulta.json()["revision"], 2)

    def test_misma_base_con_otro_estado_o_base_vieja_devuelve_409(self):
        estado1 = self._estado_mitad_turno()
        self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado1},
        )
        estado2 = copy.deepcopy(estado1)
        estado2["accionesRestantes"] = 0

        conflicto = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado2},
        )
        self.assertEqual(conflicto.status_code, 409)
        self.assertEqual(conflicto.json()["codigo"], "REVISION_INCOMPATIBLE")

    def test_estado_invalido_devuelve_400(self):
        estado_malo = copy.deepcopy(self.estado_inicial)
        estado_malo["ronda"] = 9
        respuesta = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado_malo},
        )
        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.json()["codigo"], "ESTADO_INVALIDO")

        final_malo = copy.deepcopy(self.estado_inicial)
        final_malo["estado"] = "FINALIZADA"
        final_malo["ronda"] = 4
        respuesta_final = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": final_malo},
        )
        self.assertEqual(respuesta_final.status_code, 400)

    def test_partida_inexistente_devuelve_404(self):
        consulta = self.cliente.get("/api/v1/partidas/EX-999")
        self.assertEqual(consulta.status_code, 404)

        guardado = self.cliente.put(
            "/api/v1/partidas/EX-999/estado",
            json={"revisionBase": 1, "estado": self.estado_inicial},
        )
        self.assertEqual(guardado.status_code, 404)

    def test_finalizada_no_admite_cambios_pero_si_reintento_identico(self):
        estado_final = self._estado_finalizado()
        primero = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado_final},
        )
        self.assertEqual(primero.status_code, 200)

        reintento = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 1, "estado": estado_final},
        )
        self.assertEqual(reintento.status_code, 200)
        self.assertEqual(reintento.json(), primero.json())

        cambio = self.cliente.put(
            f"/api/v1/partidas/{self.partida_id}/estado",
            json={"revisionBase": 2, "estado": estado_final},
        )
        self.assertEqual(cambio.status_code, 409)
        self.assertEqual(cambio.json()["codigo"], "PARTIDA_FINALIZADA")


if __name__ == "__main__":
    unittest.main()
