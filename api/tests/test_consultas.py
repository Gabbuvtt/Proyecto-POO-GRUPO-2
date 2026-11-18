import copy
import os
import tempfile
import unittest

import almacen
import main
from main import ErrorApi, consultar_cofre, consultar_templo, crear_partida
from modelos import ContenidoTemplo, PaqueteCofre, PeticionPartida
from tablero import TipoCasilla, a_coordenada, buscar_casillas


class PruebasConsultas(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        almacen.usar_archivo(os.path.join(self.temp_dir.name, "expedicion.json"))
        almacen.registrar_jugador("Ana Perez", "ana_ucab")
        almacen.registrar_jugador("Luis Gomez", "luis_ucab")
        peticion = PeticionPartida(requestId="solicitud-001", jugadores=["J1", "J2"])
        self.partida_id = crear_partida(peticion)["partidaId"]
        self.partida = almacen.buscar_partida(self.partida_id)

    def tearDown(self):
        self.temp_dir.cleanup()

    def templo_sin_pista(self):
        for posicion in buscar_casillas(self.partida["tablero"], TipoCasilla.TEMPLO):
            coord = a_coordenada(posicion[0], posicion[1])
            if coord not in self.partida["pistas"]:
                return coord

    def test_templo_con_pista(self):
        coords = list(self.partida["pistas"].keys())
        for i in range(len(coords)):
            contenido = ContenidoTemplo(**consultar_templo(self.partida_id, coords[i]))
            self.assertEqual(contenido.pista.indice, i + 1)
            self.assertEqual(contenido.pista.fragmento, self.partida["pistas"][coords[i]])
            self.assertEqual(contenido.reliquia.reliquiaId, self.partida["reliquias"][coords[i]]["reliquiaId"])

    def test_templo_sin_pista(self):
        contenido = ContenidoTemplo(**consultar_templo(self.partida_id, self.templo_sin_pista()))
        self.assertIsNone(contenido.pista)
        self.assertIn(contenido.reliquia.valor, [1, 2, 3])

    def test_templo_coordenada_invalida(self):
        for coord in ["Z9", "A0", "F1", "3C", ""]:
            with self.assertRaises(ErrorApi) as error:
                consultar_templo(self.partida_id, coord)
            self.assertEqual(error.exception.estado_http, 400)

    def test_casilla_que_no_es_templo(self):
        with self.assertRaises(ErrorApi) as error:
            consultar_templo(self.partida_id, self.partida["campamento"])
        self.assertEqual(error.exception.estado_http, 400)

    def test_templo_partida_inexistente(self):
        with self.assertRaises(ErrorApi) as error:
            consultar_templo("EX-999", "A1")
        self.assertEqual(error.exception.estado_http, 404)

    def test_cofre_devuelve_el_paquete(self):
        paquete = PaqueteCofre(**consultar_cofre(self.partida_id))
        self.assertEqual(paquete.partidaId, self.partida_id)
        self.assertEqual(paquete.iteraciones, 100000)
        respuesta = consultar_cofre(self.partida_id)
        for campo in ["contrasena", "clave", "reliquiaId", "valor"]:
            self.assertNotIn(campo, respuesta)

    def test_cofre_partida_inexistente(self):
        with self.assertRaises(ErrorApi) as error:
            consultar_cofre("EX-999")
        self.assertEqual(error.exception.estado_http, 404)

    def test_consultas_no_modifican_la_partida(self):
        antes = copy.deepcopy(self.partida)
        consultar_templo(self.partida_id, self.templo_sin_pista())
        consultar_cofre(self.partida_id)
        self.assertEqual(self.partida, antes)


class PruebasSwagger(unittest.TestCase):

    def test_documenta_400_y_no_422(self):
        rutas = main.app.openapi()["paths"]
        for ruta in rutas.values():
            for operacion in ruta.values():
                self.assertIn("400", operacion["responses"])
                self.assertNotIn("422", operacion["responses"])


if __name__ == "__main__":
    unittest.main()
