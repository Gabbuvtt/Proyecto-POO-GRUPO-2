package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class ServicioCriptograficoTest {

    // vector de prueba del Anexo B
    static final String SAL = "xBxRniX+mjbP3gI6qy93zA==";
    static final String NONCE = "BVmBDNDCL0Gw5CM6";
    static final String CIFRADO =
            "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0"
                    + "pikC+bkl1DmSNPrLlES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ==";

    static final String JSON_ESPERADO =
            "{\"partidaId\":\"EX-RED-01\",\"cofreId\":\"COFRE-01\",\"reliquiaId\":\"REL-ESPECIAL\",\"valor\":4}";

    private final ServicioCriptografico servicio = new ServicioCriptografico();

    // arma el paquete del Anexo B cambiando solo los campos que recibe
    private PaqueteCofre paquete(String partidaId, String sal, String nonce, String cifrado) {
        return new PaqueteCofre(partidaId, "COFRE-01", "AES/GCM/NoPadding", "PBKDF2WithHmacSHA256",
                100000, 256, sal, nonce, 128, "RVgtUkVELTAxfENPRlJFLTAx", cifrado);
    }

    private PaqueteCofre paqueteAnexoB() {
        return paquete("EX-RED-01", SAL, NONCE, CIFRADO);
    }

    @Test
    void contrasenaCorrectaRecuperaElJson() throws Exception {
        assertEquals(JSON_ESPERADO, servicio.descifrar(paqueteAnexoB(), "ORBITA-7392"));
    }

    @Test
    void contrasenaSinGuionNoAutentica() {
        assertThrows(FalloAutenticacionException.class,
                () -> servicio.descifrar(paqueteAnexoB(), "ORBITA7392"));
    }

    @Test
    void byteAlteradoNoAutentica() {
        byte[] bytes = Base64.getDecoder().decode(CIFRADO);
        bytes[0] = (byte) (bytes[0] ^ 1);
        String alterado = Base64.getEncoder().encodeToString(bytes);
        assertThrows(FalloAutenticacionException.class,
                () -> servicio.descifrar(paquete("EX-RED-01", SAL, NONCE, alterado), "ORBITA-7392"));
    }

    @Test
    void base64RotoEsCorrupto() {
        assertThrows(PaqueteCorruptoException.class,
                () -> servicio.descifrar(paquete("EX-RED-01", "%%no-es-base64%%", NONCE, CIFRADO), "ORBITA-7392"));
    }

    @Test
    void nonceDeOtroTamanoEsCorrupto() {
        String nonceCorto = Base64.getEncoder().encodeToString(new byte[8]);
        assertThrows(PaqueteCorruptoException.class,
                () -> servicio.descifrar(paquete("EX-RED-01", SAL, nonceCorto, CIFRADO), "ORBITA-7392"));
    }

    @Test
    void campoAusenteEsCorrupto() {
        assertThrows(PaqueteCorruptoException.class,
                () -> servicio.descifrar(paquete("EX-RED-01", null, NONCE, CIFRADO), "ORBITA-7392"));
    }

    @Test
    void idsQueNoCoincidenConElAadSonCorruptos() {
        assertThrows(PaqueteCorruptoException.class,
                () -> servicio.descifrar(paquete("OTRA-PARTIDA", SAL, NONCE, CIFRADO), "ORBITA-7392"));
    }
}
