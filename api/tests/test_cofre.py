import base64
import unittest

from cryptography.exceptions import InvalidTag

from cofre import cifrar_cofre, descifrar_cofre

# vector de prueba del Anexo B
PAQUETE_ANEXO_B = {
    "partidaId": "EX-RED-01",
    "cofreId": "COFRE-01",
    "algoritmo": "AES/GCM/NoPadding",
    "kdf": "PBKDF2WithHmacSHA256",
    "iteraciones": 100000,
    "longitudClaveBits": 256,
    "saltBase64": "xBxRniX+mjbP3gI6qy93zA==",
    "nonceBase64": "BVmBDNDCL0Gw5CM6",
    "tagBits": 128,
    "aadBase64": "RVgtUkVELTAxfENPRlJFLTAx",
    "cifradoConTagBase64": (
        "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0"
        "pikC+bkl1DmSNPrLlES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ=="
    ),
}

CONTENIDO_ESPERADO = {
    "partidaId": "EX-RED-01",
    "cofreId": "COFRE-01",
    "reliquiaId": "REL-ESPECIAL",
    "valor": 4,
}


class PruebasAnexoB(unittest.TestCase):

    def test_contrasena_correcta(self):
        contenido = descifrar_cofre(PAQUETE_ANEXO_B, "ORBITA-7392")
        self.assertEqual(contenido, CONTENIDO_ESPERADO)

    def test_contrasena_sin_guion_no_autentica(self):
        with self.assertRaises(InvalidTag):
            descifrar_cofre(PAQUETE_ANEXO_B, "ORBITA7392")

    def test_byte_alterado_no_autentica(self):
        cifrado = bytearray(base64.b64decode(PAQUETE_ANEXO_B["cifradoConTagBase64"]))
        cifrado[0] = cifrado[0] ^ 1
        alterado = dict(PAQUETE_ANEXO_B)
        alterado["cifradoConTagBase64"] = base64.b64encode(cifrado).decode("utf-8")
        with self.assertRaises(InvalidTag):
            descifrar_cofre(alterado, "ORBITA-7392")


class PruebasCifrado(unittest.TestCase):

    def setUp(self):
        self.paquete = cifrar_cofre("EX-RED-01", "COFRE-01", "ORBITA-7392")

    def test_cifrar_y_descifrar(self):
        contenido = descifrar_cofre(self.paquete, "ORBITA-7392")
        self.assertEqual(contenido, CONTENIDO_ESPERADO)

    def test_tamanos_y_aad(self):
        self.assertEqual(len(base64.b64decode(self.paquete["saltBase64"])), 16)
        self.assertEqual(len(base64.b64decode(self.paquete["nonceBase64"])), 12)
        self.assertEqual(self.paquete["aadBase64"], PAQUETE_ANEXO_B["aadBase64"])
        self.assertEqual(self.paquete["iteraciones"], 100000)
        self.assertEqual(self.paquete["longitudClaveBits"], 256)
        self.assertEqual(self.paquete["tagBits"], 128)

    def test_no_expone_la_contrasena(self):
        for valor in self.paquete.values():
            self.assertNotIn("ORBITA", str(valor))

    def test_sal_y_nonce_cambian_en_cada_cifrado(self):
        otro = cifrar_cofre("EX-RED-01", "COFRE-01", "ORBITA-7392")
        self.assertNotEqual(self.paquete["saltBase64"], otro["saltBase64"])
        self.assertNotEqual(self.paquete["nonceBase64"], otro["nonceBase64"])


if __name__ == "__main__":
    unittest.main()
