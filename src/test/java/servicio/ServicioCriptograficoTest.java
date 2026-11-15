package servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServicioCriptograficoTest {

    // vector de prueba del Anexo B
    static final PaqueteCofre PAQUETE_ANEXO_B = new PaqueteCofre(
            "EX-RED-01",
            "COFRE-01",
            "AES/GCM/NoPadding",
            "PBKDF2WithHmacSHA256",
            100000,
            256,
            "xBxRniX+mjbP3gI6qy93zA==",
            "BVmBDNDCL0Gw5CM6",
            128,
            "RVgtUkVELTAxfENPRlJFLTAx",
            "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0"
                    + "pikC+bkl1DmSNPrLlES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ==");

    static final String JSON_ESPERADO =
            "{\"partidaId\":\"EX-RED-01\",\"cofreId\":\"COFRE-01\",\"reliquiaId\":\"REL-ESPECIAL\",\"valor\":4}";

    @Test
    void contrasenaCorrectaRecuperaElJson() throws Exception {
        ServicioCriptografico servicio = new ServicioCriptografico();
        assertEquals(JSON_ESPERADO, servicio.descifrar(PAQUETE_ANEXO_B, "ORBITA-7392"));
    }
}
