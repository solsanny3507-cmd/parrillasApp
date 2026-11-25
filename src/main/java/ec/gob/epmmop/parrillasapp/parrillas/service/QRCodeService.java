package ec.gob.epmmop.parrillasapp.parrillas.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import ec.gob.epmmop.parrillasapp.parrillas.model.ReservaParrilla;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class QRCodeService {

//    private static final String BASE_URL = "http://localhost:8082/ReservasApp/pages";
//    private static final String BASE_URL = System.getProperty("server.url") + "ReservasApp/pages";
    private static final Logger log = LoggerFactory.getLogger(QRCodeService.class);
    private static final String BASE_URL = "https://dev.epmmop.gob.ec:8443/ParrillasApp";

    public byte[] generarQR(String contenido, int ancho, int alto) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = new QRCodeWriter()
                .encode(contenido, BarcodeFormat.QR_CODE, ancho, alto, hints);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
            return out.toByteArray();
        }
    }

    public String construirContenidoQR(ReservaParrilla reserva, String autorizacion) {
        // Usamos el ID de reserva + la autorización como token de verificación
        String token = generarToken(reserva.getRepSecuencial(), autorizacion);

        return BASE_URL + "/validate.xhtml?id=" + reserva.getRepSecuencial() + "&token=" + token;
    }

    /**
     * Token simple: hash SHA-256 del id + autorización.
     * Evita que alguien adivine URLs de otras reservas.
     */
    public String generarToken(Integer idReserva, String autorizacion) {
        try {
            String input = idReserva + "|" + autorizacion;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Tomamos los primeros 16 bytes para una URL corta pero segura
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("FALLA GENERACIÓN DE TOKEN PARA URL DE CÓDIGO QR", e);
            // Fallback si SHA-256 no está disponible (no debería ocurrir)
            return autorizacion.replaceAll("[^a-zA-Z0-9]", "");
        }
    }
}
