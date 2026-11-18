package servicio;

/** Vector de prueba publico del Anexo B (partida EX-RED-01, cofre COFRE-01). */
public final class VectorAnexoB {

    /** Contrasena publicada en el Anexo B. */
    public static final String CONTRASENA = "ORBITA-7392";

    private static final String SAL = "xBxRniX+mjbP3gI6qy93zA==";
    private static final String NONCE = "BVmBDNDCL0Gw5CM6";
    private static final String AAD = "RVgtUkVELTAxfENPRlJFLTAx";
    // en el enunciado esta en dos lineas, se une sin espacios
    private static final String CIFRADO =
            "u/hosFuUuUt+RKGBQV9S/amTmkZd5SwCLtnzyn6/EgT9Y3Nmhs7G9JfRfXMZPce426aLkJRpuGdof1jG0"
                    + "pikC+bkl1DmSNPrLlES17AuJ9UZaWvXl2pgbL84kWXKITU5noqdPQ==";

    private VectorAnexoB() {
    }

    /** Arma el paquete cifrado del Anexo B. */
    public static PaqueteCofre crearPaquete() {
        return new PaqueteCofre("EX-RED-01", "COFRE-01", "AES/GCM/NoPadding", "PBKDF2WithHmacSHA256",
                100000, 256, SAL, NONCE, 128, AAD, CIFRADO);
    }
}
