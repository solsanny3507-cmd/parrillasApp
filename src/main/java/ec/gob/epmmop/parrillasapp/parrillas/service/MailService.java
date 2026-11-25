package ec.gob.epmmop.parrillasapp.parrillas.service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Date;

@ApplicationScoped
public class MailService {

    @Resource(mappedName = "java:jboss/envioMail")
    private Session mailSession;

    private static final String REMITENTE = "app-servicios@epmmop.gob.ec";

    public void enviarHtml(String destinatario, String asunto, String contenidoHtml) throws Exception {
        MimeMessage mensaje = new MimeMessage(mailSession);
        mensaje.setFrom(new InternetAddress(REMITENTE, "Verificación - EPMMOP"));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setContent(contenidoHtml, "text/html; charset=UTF-8");

        Transport.send(mensaje);
    }

    public void enviarHtmlConQR(String destinatario,
                                String asunto,
                                String contenidoHtml,
                                byte[] imagenBytes) throws Exception {

        MimeMessage mensaje = new MimeMessage(mailSession);
        mensaje.setFrom(new InternetAddress(REMITENTE, "Reserva de Parrillas - EPMMOP"));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto, "UTF-8");
        mensaje.setSentDate(new Date());

        // Parte HTML
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(contenidoHtml, "text/html; charset=UTF-8");

        // Parte imagen QR incrustada
        MimeBodyPart qrPart = new MimeBodyPart();
        DataSource qrDataSource = new ByteArrayDataSource(imagenBytes, "image/png");
        qrPart.setDataHandler(new DataHandler(qrDataSource));
        qrPart.setHeader("Content-ID", "<qrReserva>");   // debe coincidir con el cid: del HTML
        qrPart.setDisposition(MimeBodyPart.INLINE);
        qrPart.setFileName("qr-reserva.png");

        // Multipart/related agrupa HTML + imagen para que el cliente de correo
        // sepa que la imagen pertenece al cuerpo y no es un adjunto separado
        MimeMultipart multipart = new MimeMultipart("related");
        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(qrPart);

        mensaje.setContent(multipart);
        Transport.send(mensaje);
    }
}
