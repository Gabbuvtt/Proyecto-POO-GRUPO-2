import unittest

from tablero import TipoCasilla, desde_coordenada
from validaciones import distancias_desde


def tablero_ejemplo():
    # mapa propuesto: C3, X D2, templos B1, B3, C4 y A5, pantano D1
    tablero = []
    for _ in range(5):
        tablero.append([TipoCasilla.SENDERO] * 5)
    casillas = {
        "C3": TipoCasilla.CAMPAMENTO,
        "D2": TipoCasilla.COFRE,
        "B1": TipoCasilla.TEMPLO,
        "B3": TipoCasilla.TEMPLO,
        "C4": TipoCasilla.TEMPLO,
        "A5": TipoCasilla.TEMPLO,
        "D1": TipoCasilla.PANTANO,
        "A2": TipoCasilla.PANTANO,
        "D5": TipoCasilla.PANTANO,
        "E4": TipoCasilla.PANTANO,
    }
    for coordenada, tipo in casillas.items():
        fila, col = desde_coordenada(coordenada)
        tablero[fila][col] = tipo
    return tablero


def distancia(distancias, coordenada):
    fila, col = desde_coordenada(coordenada)
    return distancias[fila][col]


class PruebasDistancias(unittest.TestCase):

    def test_distancias_desde_el_campamento(self):
        distancias = distancias_desde(tablero_ejemplo(), desde_coordenada("C3"))
        self.assertEqual(distancia(distancias, "C3"), 0)
        self.assertEqual(distancia(distancias, "B3"), 1)
        self.assertEqual(distancia(distancias, "C4"), 1)
        self.assertEqual(distancia(distancias, "D2"), 2)
        self.assertEqual(distancia(distancias, "B1"), 3)
        self.assertEqual(distancia(distancias, "A5"), 4)

    def test_no_entra_en_pantanos(self):
        distancias = distancias_desde(tablero_ejemplo(), desde_coordenada("C3"))
        self.assertIsNone(distancia(distancias, "D1"))
        self.assertIsNone(distancia(distancias, "A2"))

    def test_rodea_el_pantano(self):
        # A1 queda detras de A2 (pantano), hay que ir por B1
        distancias = distancias_desde(tablero_ejemplo(), desde_coordenada("C3"))
        self.assertEqual(distancia(distancias, "A1"), 4)

    def test_casilla_encerrada_no_se_alcanza(self):
        tablero = tablero_ejemplo()
        # E5 queda rodeada por D5 y E4
        distancias = distancias_desde(tablero, desde_coordenada("C3"))
        self.assertIsNone(distancia(distancias, "E5"))


if __name__ == "__main__":
    unittest.main()
