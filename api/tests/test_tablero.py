import unittest

from tablero import (CANTIDADES, COLUMNAS, FILAS, a_coordenada,
                     desde_coordenada, vecinos)


class PruebasTablero(unittest.TestCase):

    def test_cantidades_suman_el_tablero(self):
        total = 0
        for cantidad in CANTIDADES.values():
            total += cantidad
        self.assertEqual(total, FILAS * COLUMNAS)

    def test_a_coordenada(self):
        self.assertEqual(a_coordenada(0, 0), "A1")
        self.assertEqual(a_coordenada(2, 2), "C3")
        self.assertEqual(a_coordenada(4, 4), "E5")

    def test_desde_coordenada(self):
        self.assertEqual(desde_coordenada("A1"), (0, 0))
        self.assertEqual(desde_coordenada("D2"), (1, 3))
        self.assertEqual(desde_coordenada("E5"), (4, 4))

    def test_coordenadas_invalidas(self):
        for texto in ["", "C", "F1", "A0", "A6", "3C", "c3", "AA"]:
            with self.assertRaises(ValueError):
                desde_coordenada(texto)
        with self.assertRaises(ValueError):
            a_coordenada(5, 0)

    def test_vecinos_de_esquina(self):
        self.assertEqual(sorted(vecinos(0, 0)), [(0, 1), (1, 0)])

    def test_vecinos_del_centro(self):
        self.assertEqual(len(vecinos(2, 2)), 4)


if __name__ == "__main__":
    unittest.main()
