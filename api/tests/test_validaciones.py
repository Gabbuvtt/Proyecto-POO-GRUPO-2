import unittest

from tablero import TipoCasilla, desde_coordenada
from validaciones import (
    campamento_tiene_vecinos,
    distancias_desde,
    hay_templo_cercano,
    largo_ruta,
    ruta_valida,
)


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


def poner(tablero, coordenada, tipo):
    fila, col = desde_coordenada(coordenada)
    tablero[fila][col] = tipo


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


class PruebasCampamento(unittest.TestCase):

    def test_campamento_con_vecinos_libres(self):
        self.assertTrue(campamento_tiene_vecinos(tablero_ejemplo()))

    def test_campamento_rodeado_de_pantanos(self):
        # solo C4 queda libre alrededor de C3
        tablero = tablero_ejemplo()
        poner(tablero, "B3", TipoCasilla.PANTANO)
        poner(tablero, "D3", TipoCasilla.PANTANO)
        poner(tablero, "C2", TipoCasilla.PANTANO)
        self.assertFalse(campamento_tiene_vecinos(tablero))

    def test_templo_cercano(self):
        self.assertTrue(hay_templo_cercano(tablero_ejemplo()))

    def test_templos_lejos_del_campamento(self):
        # sin B3 ni C4, el templo mas cercano B1 esta a 3
        tablero = tablero_ejemplo()
        poner(tablero, "B3", TipoCasilla.SENDERO)
        poner(tablero, "C4", TipoCasilla.SENDERO)
        self.assertFalse(hay_templo_cercano(tablero))


class PruebasRuta(unittest.TestCase):

    def test_largo_de_la_ruta(self):
        # C3-B3-C4-C3-D2-C3 = 1+2+1+2+2 = 8
        tablero = tablero_ejemplo()
        largo = largo_ruta(tablero, desde_coordenada("B3"), desde_coordenada("C4"))
        self.assertEqual(largo, 8)

    def test_ruta_corta_es_valida(self):
        tablero = tablero_ejemplo()
        self.assertTrue(ruta_valida(tablero, desde_coordenada("B3"), desde_coordenada("C4")))
        self.assertTrue(ruta_valida(tablero, desde_coordenada("C4"), desde_coordenada("B3")))

    def test_ruta_larga_no_es_valida(self):
        # con B1 y A5 la ruta suma 16 en cualquier orden
        tablero = tablero_ejemplo()
        self.assertFalse(ruta_valida(tablero, desde_coordenada("B1"), desde_coordenada("A5")))

    def test_ruta_bloqueada_no_es_valida(self):
        # A5 queda encerrado por pantanos
        tablero = tablero_ejemplo()
        poner(tablero, "A4", TipoCasilla.PANTANO)
        poner(tablero, "B5", TipoCasilla.PANTANO)
        templo1 = desde_coordenada("B3")
        templo2 = desde_coordenada("A5")
        self.assertIsNone(largo_ruta(tablero, templo1, templo2))
        self.assertFalse(ruta_valida(tablero, templo1, templo2))


if __name__ == "__main__":
    unittest.main()
