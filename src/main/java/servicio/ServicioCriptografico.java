package servicio;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Descifra el paquete del cofre con PBKDF2 y AES-GCM. */
public class ServicioCriptografico {

    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int ITERACIONES = 100000;
    private static final int LARGO_CLAVE_BITS = 256;
    private static final int TAG_BITS = 128;
    private static final int LARGO_SAL = 16;
    private static final int LARGO_NONCE = 12;
    private static final int LARGO_TAG = TAG_BITS / 8;

    /** Deriva la clave AES de 256 bits a partir de la contrasena y la sal. */
    public SecretKeySpec derivarClave(String contrasena, byte[] sal) throws GeneralSecurityException {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena.toCharArray(), sal, ITERACIONES, LARGO_CLAVE_BITS);
        SecretKeyFactory fabrica = SecretKeyFactory.getInstance(KDF);
        byte[] clave = fabrica.generateSecret(especificacion).getEncoded();
        especificacion.clearPassword();
        return new SecretKeySpec(clave, "AES");
    }

    /** Descifra el paquete con la contrasena dada y devuelve el JSON del cofre. */
    public String descifrar(PaqueteCofre paquete, String contrasena)
            throws FalloAutenticacionException, PaqueteCorruptoException {
        validarPaquete(paquete);
        byte[] sal = decodificar(paquete.getSaltBase64(), "saltBase64");
        byte[] nonce = decodificar(paquete.getNonceBase64(), "nonceBase64");
        byte[] aad = decodificar(paquete.getAadBase64(), "aadBase64");
        byte[] cifrado = decodificar(paquete.getCifradoConTagBase64(), "cifradoConTagBase64");

        try {
            SecretKeySpec clave = derivarClave(contrasena, sal);
            Cipher cifrador = Cipher.getInstance(ALGORITMO);
            cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, nonce));
            cifrador.updateAAD(aad);

            // la etiqueta va en los ultimos 16 bytes del cifrado
            byte[] texto = cifrador.doFinal(cifrado);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            // contrasena incorrecta o datos alterados
            throw new FalloAutenticacionException("No se pudo autenticar el archivo");
        } catch (GeneralSecurityException e) {
            throw new PaqueteCorruptoException("No se pudo procesar el paquete: " + e.getMessage());
        }
    }

    /** Revisa que el paquete tenga todos sus campos, los parametros esperados y tamanos correctos. */
    public void validarPaquete(PaqueteCofre paquete) throws PaqueteCorruptoException {
        if (paquete == null) {
            throw new PaqueteCorruptoException("No hay paquete");
        }
        validarCampo(paquete.getPartidaId(), "partidaId");
        validarCampo(paquete.getCofreId(), "cofreId");
        validarCampo(paquete.getAlgoritmo(), "algoritmo");
        validarCampo(paquete.getKdf(), "kdf");
        validarCampo(paquete.getSaltBase64(), "saltBase64");
        validarCampo(paquete.getNonceBase64(), "nonceBase64");
        validarCampo(paquete.getAadBase64(), "aadBase64");
        validarCampo(paquete.getCifradoConTagBase64(), "cifradoConTagBase64");

        if (!ALGORITMO.equals(paquete.getAlgoritmo()) || !KDF.equals(paquete.getKdf())) {
            throw new PaqueteCorruptoException("Algoritmo o kdf no soportado");
        }
        if (paquete.getIteraciones() != ITERACIONES || paquete.getLongitudClaveBits() != LARGO_CLAVE_BITS
                || paquete.getTagBits() != TAG_BITS) {
            throw new PaqueteCorruptoException("Parametros de cifrado incorrectos");
        }

        if (decodificar(paquete.getSaltBase64(), "saltBase64").length != LARGO_SAL) {
            throw new PaqueteCorruptoException("La sal no mide " + LARGO_SAL + " bytes");
        }
        if (decodificar(paquete.getNonceBase64(), "nonceBase64").length != LARGO_NONCE) {
            throw new PaqueteCorruptoException("El nonce no mide " + LARGO_NONCE + " bytes");
        }
        if (decodificar(paquete.getCifradoConTagBase64(), "cifradoConTagBase64").length <= LARGO_TAG) {
            throw new PaqueteCorruptoException("El cifrado es demasiado corto");
        }

        // el AAD debe ser partidaId/cofreId
        byte[] aad = decodificar(paquete.getAadBase64(), "aadBase64");
        String esperado = paquete.getPartidaId() + "|" + paquete.getCofreId();
        if (!esperado.equals(new String(aad, StandardCharsets.UTF_8))) {
            throw new PaqueteCorruptoException("El AAD no coincide con la partida y el cofre");
        }
    }

    private void validarCampo(String valor, String nombre) throws PaqueteCorruptoException {
        if (valor == null || valor.isBlank()) {
            throw new PaqueteCorruptoException("Falta el campo " + nombre);
        }
    }

    private byte[] decodificar(String texto, String nombre) throws PaqueteCorruptoException {
        try {
            return Base64.getDecoder().decode(texto);
        } catch (IllegalArgumentException e) {
            throw new PaqueteCorruptoException("Base64 invalido en " + nombre);
        }
    }
}
