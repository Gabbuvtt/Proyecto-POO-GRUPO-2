import os
import tempfile
import unittest

from fastapi.testclient import TestClient

import almacen
import main


class PruebasSalud(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        almacen.usar_archivo(os.path.join(self.temp_dir.name, "expedicion.json"))
        self.cliente = TestClient(main.app)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_salud_responde_disponible_y_contrato_v2(self):
        respuesta = self.cliente.get("/api/v1/salud")
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.json(), {"disponible": True, "versionContrato": 2})


if __name__ == "__main__":
    unittest.main()
