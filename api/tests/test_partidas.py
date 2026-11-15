import re
import unittest
from unittest import mock

import partidas
from cofre import descifrar_cofre
from partidas import COFRE_ID, GeneracionFallida, generar_partida
from tablero import CANTIDADES, TipoCasilla, buscar_casillas, desde_coordenada
from validaciones import campamento_tiene_vecinos, hay_templo_cercano, ruta_valida


class PruebasGenerarPartida(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.partida = generar_partida("PAR-001", ["J1", "J2"])

    def test_cantidades_del_tablero(self):
        for tipo, cantidad in CANTIDADES.items():
            self.assertEqual(len(buscar_casillas(self.partida["tablero"], tipo)), cantidad)

    def test_cumple_las_restricciones(self):
        tablero = self.partida["tablero"]
        templos_pista = []
        for coord in self.partida["pistas"]:
            templos_pista.append(desde_coordenada(coord))
        self.assertTrue(campamento_tiene_vecinos(tablero))
        self.assertTrue(hay_templo_cercano(tablero))
        self.assertTrue(ruta_valida(tablero, templos_pista[0], templos_pista[1]))

    def test_reliquias_y_pistas_en_templos(self):
        tablero = self.partida["tablero"]
        self.assertEqual(len(self.partida["reliquias"]), 4)
        self.assertEqual(len(self.partida["pistas"]), 2)
        for coord in self.partida["pistas"]:
            fila, col = desde_coordenada(coord)
            self.assertEqual(tablero[fila][col], TipoCasilla.TEMPLO)

    def test_jugadores_empiezan_en_campamento(self):
        self.assertEqual(sorted(self.partida["orden"]), ["J1", "J2"])
        for jugador in ["J1", "J2"]:
            self.assertEqual(self.partida["posiciones"][jugador], self.partida["campamento"])

    def test_paquete_se_descifra_con_su_contrasena(self):
        pistas = list(self.partida["pistas"].values())
        contrasena = pistas[0] + "-" + pistas[1]
        self.assertRegex(contrasena, re.compile(r"^[A-Z]{6}-[0-9]{4}$"))
        contenido = descifrar_cofre(self.partida["paquete"], contrasena)
        self.assertEqual(contenido["partidaId"], "PAR-001")
        self.assertEqual(contenido["cofreId"], COFRE_ID)

    def test_guarda_la_semilla(self):
        self.assertIsInstance(self.partida["semilla"], int)

    def test_distinta_a_la_anterior(self):
        nueva = generar_partida("PAR-003", ["J2", "J1"], self.partida)
        self.assertNotEqual(nueva["campamento"], self.partida["campamento"])
        self.assertNotEqual(nueva["cofre"], self.partida["cofre"])
        pistas_nuevas = list(nueva["pistas"].values())
        pistas_anteriores = list(self.partida["pistas"].values())
        self.assertNotEqual(pistas_nuevas[0], pistas_anteriores[0])
        self.assertNotEqual(pistas_nuevas[1], pistas_anteriores[1])

    def test_descarta_candidato_igual_al_anterior(self):
        candidato = partidas.armar_candidato(self.partida["semilla"], ["J1", "J2"])
        self.assertFalse(partidas.es_distinta(candidato, self.partida))
        self.assertTrue(partidas.es_distinta(candidato, None))

    def test_ningun_candidato_sirve(self):
        with mock.patch.object(partidas, "es_candidato_valido", return_value=False):
            with self.assertRaises(GeneracionFallida):
                generar_partida("PAR-002", ["J1", "J2"])


if __name__ == "__main__":
    unittest.main()
