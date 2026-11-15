import base64
import json
import os

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

ITERACIONES = 100000
LARGO_CLAVE = 32
LARGO_SAL = 16
LARGO_NONCE = 12
TAG_BITS = 128
RELIQUIA_ESPECIAL = "REL-ESPECIAL"
VALOR_ESPECIAL = 4


def derivar_clave(contrasena, sal):
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=LARGO_CLAVE,
        salt=sal,
        iterations=ITERACIONES,
    )
    return kdf.derive(contrasena.encode("utf-8"))


def a_base64(datos):
    return base64.b64encode(datos).decode("utf-8")


def cifrar_cofre(partida_id, cofre_id, contrasena):
    # sal y nonce aleatorios seguros, no dependen de la semilla del mapa
    sal = os.urandom(LARGO_SAL)
    nonce = os.urandom(LARGO_NONCE)
    aad = (partida_id + "|" + cofre_id).encode("utf-8")
    contenido = {
        "partidaId": partida_id,
        "cofreId": cofre_id,
        "reliquiaId": RELIQUIA_ESPECIAL,
        "valor": VALOR_ESPECIAL,
    }
    texto = json.dumps(contenido, separators=(",", ":")).encode("utf-8")

    # AES-GCM deja la etiqueta de 16 bytes al final del cifrado
    clave = derivar_clave(contrasena, sal)
    cifrado = AESGCM(clave).encrypt(nonce, texto, aad)

    return {
        "partidaId": partida_id,
        "cofreId": cofre_id,
        "algoritmo": "AES/GCM/NoPadding",
        "kdf": "PBKDF2WithHmacSHA256",
        "iteraciones": ITERACIONES,
        "longitudClaveBits": LARGO_CLAVE * 8,
        "saltBase64": a_base64(sal),
        "nonceBase64": a_base64(nonce),
        "tagBits": TAG_BITS,
        "aadBase64": a_base64(aad),
        "cifradoConTagBase64": a_base64(cifrado),
    }


def descifrar_cofre(paquete, contrasena):
    # solo para comprobar el paquete antes de aceptar la partida
    sal = base64.b64decode(paquete["saltBase64"])
    nonce = base64.b64decode(paquete["nonceBase64"])
    aad = base64.b64decode(paquete["aadBase64"])
    cifrado = base64.b64decode(paquete["cifradoConTagBase64"])
    clave = derivar_clave(contrasena, sal)
    texto = AESGCM(clave).decrypt(nonce, cifrado, aad)
    return json.loads(texto.decode("utf-8"))
