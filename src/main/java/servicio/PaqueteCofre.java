package servicio;

/** Paquete cifrado del cofre, con los campos del Anexo B. */
public class PaqueteCofre {

    private String partidaId;
    private String cofreId;
    private String algoritmo;
    private String kdf;
    private int iteraciones;
    private int longitudClaveBits;
    private String saltBase64;
    private String nonceBase64;
    private int tagBits;
    private String aadBase64;
    private String cifradoConTagBase64;

    /** Crea el paquete con todos los campos del Anexo B. */
    public PaqueteCofre(String partidaId, String cofreId, String algoritmo, String kdf,
                        int iteraciones, int longitudClaveBits, String saltBase64, String nonceBase64,
                        int tagBits, String aadBase64, String cifradoConTagBase64) {
        this.partidaId = partidaId;
        this.cofreId = cofreId;
        this.algoritmo = algoritmo;
        this.kdf = kdf;
        this.iteraciones = iteraciones;
        this.longitudClaveBits = longitudClaveBits;
        this.saltBase64 = saltBase64;
        this.nonceBase64 = nonceBase64;
        this.tagBits = tagBits;
        this.aadBase64 = aadBase64;
        this.cifradoConTagBase64 = cifradoConTagBase64;
    }

    /** @return id de la partida */
    public String getPartidaId() {
        return partidaId;
    }

    /** @return id del cofre */
    public String getCofreId() {
        return cofreId;
    }

    /** @return algoritmo de cifrado */
    public String getAlgoritmo() {
        return algoritmo;
    }

    /** @return funcion de derivacion de clave */
    public String getKdf() {
        return kdf;
    }

    /** @return iteraciones de PBKDF2 */
    public int getIteraciones() {
        return iteraciones;
    }

    /** @return largo de la clave en bits */
    public int getLongitudClaveBits() {
        return longitudClaveBits;
    }

    /** @return sal en Base64 */
    public String getSaltBase64() {
        return saltBase64;
    }

    /** @return nonce en Base64 */
    public String getNonceBase64() {
        return nonceBase64;
    }

    /** @return largo de la etiqueta en bits */
    public int getTagBits() {
        return tagBits;
    }

    /** @return AAD en Base64 */
    public String getAadBase64() {
        return aadBase64;
    }

    /** @return cifrado con la etiqueta al final, en Base64 */
    public String getCifradoConTagBase64() {
        return cifradoConTagBase64;
    }
}
