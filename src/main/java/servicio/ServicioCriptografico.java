package servicio;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
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

    /** Deriva la clave AES de 256 bits a partir de la contrasena y la sal. */
    public SecretKeySpec derivarClave(String contrasena, byte[] sal) throws GeneralSecurityException {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena.toCharArray(), sal, ITERACIONES, LARGO_CLAVE_BITS);
        SecretKeyFactory fabrica = SecretKeyFactory.getInstance(KDF);
        byte[] clave = fabrica.generateSecret(especificacion).getEncoded();
        especificacion.clearPassword();
        return new SecretKeySpec(clave, "AES");
    }

    /** Descifra el paquete con la contrasena dada y devuelve el JSON del cofre. */
    public String descifrar(PaqueteCofre paquete, String contrasena) throws GeneralSecurityException {
        Base64.Decoder decodificador = Base64.getDecoder();
        byte[] sal = decodificador.decode(paquete.getSaltBase64());
        byte[] nonce = decodificador.decode(paquete.getNonceBase64());
        byte[] aad = decodificador.decode(paquete.getAadBase64());
        byte[] cifrado = decodificador.decode(paquete.getCifradoConTagBase64());

        SecretKeySpec clave = derivarClave(contrasena, sal);
        Cipher cifrador = Cipher.getInstance(ALGORITMO);
        cifrador.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, nonce));
        cifrador.updateAAD(aad);

        // la etiqueta va en los ultimos 16 bytes del cifrado
        byte[] texto = cifrador.doFinal(cifrado);
        return new String(texto, StandardCharsets.UTF_8);
    }
}
