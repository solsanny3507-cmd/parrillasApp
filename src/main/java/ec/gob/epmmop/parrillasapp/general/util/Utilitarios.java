package ec.gob.epmmop.parrillasapp.general.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.gob.epmmop.parrillasapp.general.model.Ciudadano;
import ec.gob.epmmop.parrillasapp.general.model.Transaccion;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Utilitarios {

    private static final String URL_BASE = "https://wsdinardap.epmmop.gob.ec:8445/ServiciosDinardapWS/rest/consultasCantuna/consultarRegistroCivil?cedula=";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String PENDING  = "PENDING";

    protected static final String APROBADO = "APROBADO";
    protected static final String RECHAZADO = "RECHAZADO";
    protected static final String PENDIENTE  = "PENDIENTE";


    protected String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        return Arrays.stream(texto.trim().toLowerCase().split("\\s+"))
                .map(p -> p.substring(0, 1).toUpperCase() + p.substring(1))
                .collect(Collectors.joining(" "));
    }

    protected LocalDateTime obtenerFechaActual() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String formattedDateTime = now.format(formatter);
        return LocalDateTime.parse(formattedDateTime, formatter);
    }

    protected String obtenerIP() throws UnknownHostException {
        InetAddress ip = InetAddress.getLocalHost();
        return ip.getHostAddress();
    }

    public static String obtenerNombrePorCedula(String cedula) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_BASE + cedula))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());

                boolean status = root.path("status").asBoolean();

                if (status) {
                    return root.path("data")
                            .path("persona")
                            .path("nombrePersona")
                            .asText();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigDecimal obtenerIvaRate(BigDecimal ivaPorcentaje) {
        return ivaPorcentaje.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularIva(BigDecimal precio, BigDecimal ivaPorcentaje) {
        BigDecimal base = calcularBaseImponible(precio, ivaPorcentaje);
        return precio.subtract(base).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularBaseImponible(BigDecimal precio, BigDecimal ivaPorcentaje) {
        BigDecimal ivaRate = obtenerIvaRate(ivaPorcentaje);
        BigDecimal divisor = BigDecimal.ONE.add(ivaRate);
        return precio.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    public static Boolean esCorreoValido(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            return false;
        }
        return correo.matches(EMAIL_REGEX);
    }

    public int generarTokenNumerico() {
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000); // asegura 6 dígitos
        return numero;
    }

    public String transformarStatusReserva(String statusRespuesta) {
        if (statusRespuesta == null) return "DESCONOCIDO";

        return switch (statusRespuesta) {
            case APPROVED -> "APROBADO";
            case REJECTED -> "RECHAZADO";
            case PENDING  -> "PENDIENTE";
            default -> "ERROR";
        };
    }

    public OffsetDateTime convertirStringToDate(String date) {
        return OffsetDateTime.parse(date);
    }

    public String construirHtmlReserva(Transaccion datos) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        body { font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0; }
                        .container { max-width:600px; margin:30px auto; background:#fff;
                                     border-radius:8px; box-shadow:0 2px 8px rgba(0,0,0,0.1); overflow:hidden; }
                        .header { background:#254085; color:white; padding:24px; text-align:center; }
                        .header h1 { margin:0; font-size:22px; }
                        .header p  { margin:6px 0 0; font-size:13px; opacity:.85; }
                        .body { padding:28px; color:#333; font-size:14px; line-height:1.6; }
                        .qr-box { text-align:center; margin:24px 0; }
                        .qr-box img { width:220px; height:220px; border:3px solid #1a5276;
                                      border-radius:8px; padding:8px; }
                        .qr-box p { margin:8px 0 0; color:#777; font-size:12px; }
                        .info-box { background:#f0f3f4; border-left:4px solid #1a5276;
                                    padding:14px 18px; border-radius:4px; margin:18px 0; }
                        .info-box p { margin:5px 0; }
                        .badge { display:inline-block; background:#1e8449; color:white;
                                 padding:4px 14px; border-radius:20px; font-size:12px;
                                 font-weight:bold; letter-spacing:.5px; }
                        .footer { text-align:center; font-size:11px; color:#aaa;
                                  padding:16px; border-top:1px solid #eee; }
                        .logo-top {text-align: center; margin-bottom: 20px;}
                        .logo-top img {max-width: 220px;height: auto;}
                        .icono {font-size: 14px;margin-right: 4px;}
                    </style>
                </head>
                <body>
                <div class="container">
                    <div class="header">
                        <h3>✅ Reserva Confirmada</h3>
                    </div>
                    <div class="body">
                
                        <div class="logo-top">
                             <img src="https://www.epmmop.gob.ec/epmmop/images/banners/logoReportes.jpg"
                                         alt="EPMMOP"/>
                        </div>
                
                        <p>Estimado/a <strong>%s</strong>,</p>
                        <p>Su pago fue procesado exitosamente. A continuación encontrará
                           el código QR que deberá presentar al personal para validar
                           su reserva al momento del ingreso:</p>
                
                        <div class="qr-box">
                            <img src="cid:qrReserva" alt="Código QR de validación"/>
                            <p>Escanee este código para verificar su reserva</p></br>
                            <span class="badge">RESERVA ACTIVA</span>
                        </div>
                
                        <div class="info-box">
                            <p><strong><span class=icono>🌳</span> Parque:</strong> %s</p>
                            <p><strong><span class=icono>📍</span> Grupo:</strong> %s</p>
                            <p><strong><span class=icono>🔥</span> Parrilla:</strong> %s</p>
                            <p><strong><span class=icono>📅</span> Fecha de reserva:</strong> %s</p>
                            <p><strong><span class=icono>👤</span> Titular de la reserva:</strong> %s</p>
                        </div>
                
                        <p style="color:#888; font-size:12px;">
                            Este código QR es único e intransferible y corresponde
                            exclusivamente a esta reserva. No lo comparta con terceros.
                        </p>
                        
                        <div class="info-box" style="font-size:12px; line-height:1.5;">
                            <p><strong>📋 Políticas de Uso</strong></p>
                            <ul style="padding-left:18px; margin:8px 0;">
                                <li>El uso de la parrilla es exclusivo para quienes realicen la reserva.</li>
                                <li>Respetar la capacidad máxima.</li>
                                <li>Horario de uso: 09h00 a 17h00 (estricto).</li>
                                <li>Mantener limpio el lugar y no dejar basura.</li>
                                <li>Las mascotas deben permanecer con correa y sus dueños portar funda para recoger desechos.</li>
                                <li>No ingerir bebidas alcohólicas.</li>
                                <li>No dañar las instalaciones.</li>
                                <li>No agredir flora ni fauna nativa.</li>
                                <li>Apagar con agua el carbón al finalizar y dejar el asadero limpio.</li>
                                <li>Usar únicamente el asadero asignado.</li>
                                <li>No utilizar confeti ni dejar decoraciones.</li>
                                <li>Prohibido el uso de carpas, inflables o estructuras comerciales.</li>
                            </ul>
                
                            <p style="margin-top:10px;"><strong>📌 Políticas de Reserva</strong></p>
                            <ul style="padding-left:18px; margin:8px 0;">
                                <li>El uso tiene un costo que debe pagarse en línea.</li>
                                <li>Debe presentarse en administración antes del ingreso.</li>
                                <li>No hay cambios ni devoluciones.</li>
                                <li>La fecha solo puede modificarse en caso de emergencia.</li>
                                <li>El titular es responsable de sus invitados.</li>
                            </ul>
                        </div>
                        
                    </div>
                    <div class="footer">
                        EPMMOP © %d — Correo generado automáticamente, por favor no responda este mensaje.
                    </div>
                </div>
                </body>
                </html>
                """.formatted(
                datos.getCiudadano().getCiuNombre(),
                datos.getReservaParrilla().getParrilla().getGrupoParrilla().getParque().getParNombre(),
                datos.getReservaParrilla().getParrilla().getGrupoParrilla().getGraNombre(),
                datos.getReservaParrilla().getParrilla().getAsaNombre(),
                datos.getReservaParrilla().getRepFechaReserva(),
                datos.getCiudadano().getCiuNombre(),
                java.time.Year.now().getValue()
        );
    }

    public String construirHtmlVerificacion(Ciudadano ciudadano) {
        return """
                <html>
                <head>
                    <meta charset="UTF-8"/>
                    <style>
                        .logo-top {text-align: center; margin-bottom: 10px; margin-top:20px}
                        .logo-top img {max-width: 220px;height: auto;}
                    </style>
                </head>
                <body style="margin:0; padding:0; background-color:#f4f6f9; font-family:Arial, sans-serif;">
                
                <div style="max-width:600px; margin:0 auto; padding:20px;">
                
                    <!-- CARD -->
                    <div style="background:#ffffff; border-radius:12px; overflow:hidden; 
                                box-shadow:0 8px 20px rgba(0,0,0,0.08);">
                
                        <!-- HEADER -->
                        <div style="background:#244084; padding:10px; text-align:center;">
                            <h3 style="color:#ffffff; font-weight:bold;"><span style="font-size:16px">🔐</span> Código de Verificación</h3>
                        </div>
                
                
                        <!-- BODY -->
                        <div style="padding:30px; text-align:center;">
                
                            <div class="logo-top">
                                <img src="https://www.epmmop.gob.ec/epmmop/images/banners/logoReportes.jpg"
                                         alt="EPMMOP"/>
                            </div>
                
                            <h2 style="color:#244084; margin-bottom:10px;">
                                Hola, %s 👋
                            </h2>
                
                            <p style="font-size:16px; color:#555; margin-bottom:20px;">
                                Hemos generado un código de verificación para continuar con tu proceso.
                            </p>
                
                            <p style="font-size:14px; color:#777;">
                                Tu código es:
                            </p>
                
                            <!-- CODIGO -->
                            <div style="margin:25px 0;">
                                <span style="
                                    display:inline-block;
                                    padding:15px 25px;
                                    background:#f1f5fb;
                                    border-radius:10px;
                                    font-size:32px;
                                    font-weight:bold;
                                    letter-spacing:8px;
                                    color:#244084;
                                    border:1px dashed #244084;
                                ">
                                    %s
                                </span>
                            </div>
                
                            <!-- MENSAJE -->
                            <p style="font-size:14px; color:#666;">
                                Este código tiene una vigencia limitada.<br/>
                                No lo compartas con nadie.
                            </p>
                
                            <!-- BOTON (decorativo UX) -->
                            <div style="margin-top:25px;">
                                <span style="
                                    display:inline-block;
                                    background:#244084;
                                    color:#fff;
                                    padding:12px 20px;
                                    border-radius:6px;
                                    font-size:14px;
                                ">
                                    Ingresar código en la aplicación
                                </span>
                            </div>
                
                        </div>
                
                        <!-- FOOTER -->
                        <div style="background:#f9f9f9; padding:15px; text-align:center;">
                            <p style="font-size:12px; color:#999; margin:0;">
                                Si no solicitaste este código, puedes ignorar este mensaje.
                            </p>
                
                            <p style="font-size:11px; color:#bbb; margin-top:8px;">
                                EPMMOP © %d — Correo automático, no responder.
                            </p>
                        </div>
                
                    </div>
                
                </div>
                
                </body>
                </html>
                """.formatted(
                ciudadano.getCiuNombre(),
                ciudadano.getUsuario().getUsxToken(),
                java.time.Year.now().getValue()
        );
    }

}
