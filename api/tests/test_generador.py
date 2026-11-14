import random
import re
import unittest

from generador import (
    elegir_templos_pista,
    generar_fragmentos,
    generar_tablero,
    repartir_reliquias,
)
from tablero import CANTIDADES, COLUMNAS, FILAS, TipoCasilla, buscar_casillas


class PruebasGenerador(unittest.TestCase):

    def test_tamano_del_tablero(self):
        tablero = generar_tablero(1)
        self.assertEqual(len(tablero), FILAS)
        for fila in tablero:
            self.assertEqual(len(fila), COLUMNAS)

    def test_cantidades_exactas(self):
        for semilla in range(20):
            tablero = generar_tablero(semilla)
            for tipo, cantidad in CANTIDADES.items():
                self.assertEqual(len(buscar_casillas(tablero, tipo)), cantidad)

    def test_misma_semilla_mismo_tablero(self):
        self.assertEqual(generar_tablero(7), generar_tablero(7))

    def test_semillas_distintas_cambian_el_tablero(self):
        self.assertNotEqual(generar_tablero(1), generar_tablero(2))


class PruebasContenidoTemplos(unittest.TestCase):

    def setUp(self):
        self.templos = buscar_casillas(generar_tablero(3), TipoCasilla.TEMPLO)

    def test_valores_de_las_reliquias(self):
        for semilla in range(20):
            reliquias = repartir_reliquias(self.templos, random.Random(semilla))
            valores = []
            ids = []
            for reliquia in reliquias.values():
                valores.append(reliquia["valor"])
                ids.append(reliquia["reliquiaId"])
            self.assertEqual(sorted(valores), [1, 2, 2, 3])
            self.assertEqual(sorted(ids), ["REL-01", "REL-02", "REL-03", "REL-04"])
            self.assertEqual(sorted(reliquias.keys()), sorted(self.templos))

    def test_templos_con_pista_distintos(self):
        for semilla in range(20):
            elegidos = elegir_templos_pista(self.templos, random.Random(semilla))
            self.assertEqual(len(elegidos), 2)
            self.assertNotEqual(elegidos[0], elegidos[1])
            self.assertIn(elegidos[0], self.templos)
            self.assertIn(elegidos[1], self.templos)

    def test_formato_de_los_fragmentos(self):
        for semilla in range(50):
            pista1, pista2 = generar_fragmentos(random.Random(semilla))
            self.assertRegex(pista1, re.compile(r"^[A-Z]{6}$"))
            self.assertRegex(pista2, re.compile(r"^[0-9]{4}$"))


if __name__ == "__main__":
    unittest.main()
