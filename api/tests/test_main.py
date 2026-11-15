import unittest
from unittest import mock

import almacen
import main
from main import ErrorApi, crear_partida
from modelos import PartidaPublica, PeticionPartida
from partidas import GeneracionFallida


def peticion(jugadores, request_id="solicitud-001"):
    return PeticionPartida(requestId=request_id, jugadores=jugadores)


class PruebasCrearPartida(unittest.TestCase):

    def setUp(self):
        almacen.partidas.clear()
        almacen.solicitudes.clear()

    def test_crea_partida_publica(self):
        respuesta = crear_partida(peticion(["J1", "J2"]))
        publica = PartidaPublica(**respuesta)
        self.assertEqual(publica.partidaId, "EX-001")
        self.assertEqual(len(publica.terreno), 5)
        self.assertEqual(sorted(publica.orden), ["J1", "J2"])
        self.assertEqual(publica.estado.ronda, 1)
        self.assertIsNotNone(almacen.buscar_partida("EX-001"))

    def test_no_expone_datos_secretos(self):
        respuesta = crear_partida(peticion(["J1", "J2"]))
        for campo in ["reliquias", "pistas", "paquete", "semilla"]:
            self.assertNotIn(campo, respuesta)

    def test_numera_las_partidas(self):
        crear_partida(peticion(["J1", "J2"], "solicitud-001"))
        respuesta = crear_partida(peticion(["J1", "J2"], "solicitud-002"))
        self.assertEqual(respuesta["partidaId"], "EX-002")

    def test_busca_la_partida_anterior_del_par(self):
        crear_partida(peticion(["J1", "J2"], "solicitud-001"))
        crear_partida(peticion(["J1", "J3"], "solicitud-002"))
        crear_partida(peticion(["J2", "J1"], "solicitud-003"))
        self.assertEqual(almacen.partida_anterior(["J1", "J2"])["partidaId"], "EX-003")
        self.assertEqual(almacen.partida_anterior(["J1", "J3"])["partidaId"], "EX-002")
        self.assertIsNone(almacen.partida_anterior(["J2", "J3"]))

    def test_mismo_request_id_y_cuerpo_devuelve_la_misma(self):
        primera = crear_partida(peticion(["J1", "J2"]))
        segunda = crear_partida(peticion(["J1", "J2"]))
        self.assertEqual(primera, segunda)
        self.assertEqual(almacen.contar_partidas(), 1)

    def test_mismo_request_id_con_otro_cuerpo(self):
        crear_partida(peticion(["J1", "J2"]))
        with self.assertRaises(ErrorApi) as error:
            crear_partida(peticion(["J1", "J3"]))
        self.assertEqual(error.exception.estado_http, 409)
        self.assertEqual(almacen.contar_partidas(), 1)

    def test_generacion_fallida_no_guarda_la_solicitud(self):
        with mock.patch.object(main, "generar_partida", side_effect=GeneracionFallida()):
            with self.assertRaises(ErrorApi):
                crear_partida(peticion(["J1", "J2"]))
        self.assertIsNone(almacen.buscar_solicitud("solicitud-001"))

    def test_jugadores_repetidos(self):
        with self.assertRaises(ErrorApi) as error:
            crear_partida(peticion(["J1", "J1"]))
        self.assertEqual(error.exception.estado_http, 400)

    def test_cantidad_de_jugadores(self):
        with self.assertRaises(ErrorApi) as error:
            crear_partida(peticion(["J1"]))
        self.assertEqual(error.exception.estado_http, 400)

    def test_jugador_inexistente(self):
        with mock.patch.object(main, "jugador_existe", return_value=False):
            with self.assertRaises(ErrorApi) as error:
                crear_partida(peticion(["J1", "J2"]))
        self.assertEqual(error.exception.estado_http, 404)

    def test_generacion_fallida_no_guarda(self):
        with mock.patch.object(main, "generar_partida", side_effect=GeneracionFallida()):
            with self.assertRaises(ErrorApi) as error:
                crear_partida(peticion(["J1", "J2"]))
        self.assertEqual(error.exception.estado_http, 503)
        self.assertEqual(almacen.contar_partidas(), 0)


if __name__ == "__main__":
    unittest.main()
