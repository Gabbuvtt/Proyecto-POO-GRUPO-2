import unittest

from generador import generar_tablero
from tablero import CANTIDADES, COLUMNAS, FILAS, buscar_casillas


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


if __name__ == "__main__":
    unittest.main()
