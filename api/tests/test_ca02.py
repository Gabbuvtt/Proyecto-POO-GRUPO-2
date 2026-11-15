import unittest

import almacen
from main import crear_partida
from modelos import PeticionPartida
from tablero import CANTIDADES, buscar_casillas, desde_coordenada
from validaciones import campamento_tiene_vecinos, hay_templo_cercano, ruta_valida

PARTIDAS_A_CREAR = 3


class PruebasCA02(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        almacen.partidas.clear()
        almacen.solicitudes.clear()
        # tres partidas seguidas del mismo par
        cls.partidas = []
        for i in range(PARTIDAS_A_CREAR):
            peticion = PeticionPartida(requestId="ca02-" + str(i), jugadores=["J1", "J2"])
            partida_id = crear_partida(peticion)["partidaId"]
            cls.partidas.append(almacen.buscar_partida(partida_id))

    def test_se_crean_tres_partidas_distintas(self):
        ids = set()
        for partida in self.partidas:
            ids.add(partida["partidaId"])
        self.assertEqual(len(ids), PARTIDAS_A_CREAR)

    def test_cantidades_exactas(self):
        for partida in self.partidas:
            for tipo, cantidad in CANTIDADES.items():
                self.assertEqual(len(buscar_casillas(partida["tablero"], tipo)), cantidad)

    def test_cumplen_las_restricciones(self):
        for partida in self.partidas:
            tablero = partida["tablero"]
            templos_pista = []
            for coord in partida["pistas"]:
                templos_pista.append(desde_coordenada(coord))
            self.assertTrue(campamento_tiene_vecinos(tablero))
            self.assertTrue(hay_templo_cercano(tablero))
            self.assertTrue(ruta_valida(tablero, templos_pista[0], templos_pista[1]))

    def test_cambian_c_x_y_pistas(self):
        for i in range(1, PARTIDAS_A_CREAR):
            anterior = self.partidas[i - 1]
            actual = self.partidas[i]
            self.assertNotEqual(actual["campamento"], anterior["campamento"])
            self.assertNotEqual(actual["cofre"], anterior["cofre"])
            pistas_anteriores = list(anterior["pistas"].values())
            pistas_actuales = list(actual["pistas"].values())
            self.assertNotEqual(pistas_actuales[0], pistas_anteriores[0])
            self.assertNotEqual(pistas_actuales[1], pistas_anteriores[1])


if __name__ == "__main__":
    unittest.main()
