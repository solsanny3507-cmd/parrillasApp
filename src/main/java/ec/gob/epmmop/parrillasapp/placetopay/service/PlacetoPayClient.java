package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.general.repository.ParametrosRepository;
import ec.gob.epmmop.parrillasapp.placetopay.config.PagoException;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PagoResponseDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayRequest;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.StringReader;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class PlacetoPayClient {

    @Inject
    private ParametrosRepository parametrosRepository;

    @Inject
    private Client client;

    private static final Logger LOGGER = Logger.getLogger(PlacetoPayClient.class.getName());

    private static final String ROUTE = "api/session";
    private final Jsonb jsonb = JsonbBuilder.create(
            new jakarta.json.bind.JsonbConfig().withFormatting(true)
    );

    public PlacetoPayResponse crearSesion(PlacetoPayRequest request) {
        //Convertir request a JSON
        String requestJson = jsonb.toJson(request);

        var parametros = parametrosRepository.findByPrmSecuencial(1);
        String API_CONNECT = parametros.getPrmUrl() + ROUTE;

        // LOGS TEMPORALES PARA DIAGNÓSTICO
        LOGGER.info("========== PLACETOPAY DEBUG ==========");
        LOGGER.info("URL obtenida de PostgreSQL: [" + parametros.getPrmUrl() + "]");
        LOGGER.info("Ruta configurada (ROUTE): [" + ROUTE + "]");
        LOGGER.info("URL FINAL CONSTRUIDA: [" + API_CONNECT + "]");
        LOGGER.info("======================================");

        // Enviar request a la API de Placetopay
        Response response = client
                .target(API_CONNECT)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(requestJson));

        // Leer respuesta como String (para imprimir tal cual)
        String responseJson = response.readEntity(String.class);
        PlacetoPayResponse placetoPayResponse = jsonb.fromJson(responseJson, PlacetoPayResponse.class);
        placetoPayResponse.setReference(request.getPayment().getReference());
        placetoPayResponse.setDescription(request.getPayment().getDescription());

        // Manejo de error
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            String mensaje = placetoPayResponse.getStatus().getMessage();
            throw new PagoException(mensaje);
        }

        return placetoPayResponse;
    }

    public String generarJsonConsulta() {
        try {
            var parametros = parametrosRepository.findByPrmSecuencial(1);

            String login = parametros.getPrmLogin();
            String secretKey = parametros.getPrmSecretKey();

            // Fecha ISO
            String seed = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                    .format(new Date());

            // Nonce
            String rawNonce = String.valueOf((int) (Math.random() * 1000000000 + 1));

            // Generar tranKey
            String concatenacion = rawNonce + seed + secretKey;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(concatenacion.getBytes("UTF-8"));

            String tranKey = Base64.getEncoder().encodeToString(hash);
            String nonce = Base64.getEncoder().encodeToString(rawNonce.getBytes());

            // Construir JSON
            JsonObject json = Json.createObjectBuilder()
                    .add("locale", "es_EC")
                    .add("auth", Json.createObjectBuilder()
                            .add("login", login)
                            .add("tranKey", tranKey)
                            .add("nonce", nonce)
                            .add("seed", seed)
                    ).build();

            return json.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generando JSON", e);
            return "{}";
        }
    }

    public PagoResponseDTO consultarPago(Integer requestId, String jsonAuth){
        try {
            var parametros = parametrosRepository.findByPrmSecuencial(1);

            String API_CONNECT = parametros.getPrmUrl() + ROUTE + "/" + requestId;

            JsonObject fullJson = Json.createReader(new StringReader(jsonAuth)).readObject();
            JsonObject cleanJson = Json.createObjectBuilder()
                    .add("auth", fullJson.getJsonObject("auth"))
                    .build();

            String finalJson = cleanJson.toString();

            Response response = client
                    .target(API_CONNECT)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(finalJson));

            String responseJson = response.readEntity(String.class);

            PagoResponseDTO dto = mapResponse(responseJson);

            // Validación básica
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                LOGGER.severe("Error HTTP: " + response.getStatus());
                dto.setMessage("Error HTTP: " + response.getStatus());
            }

            return dto;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error consultando pago", e);

            // Retorno seguro (nunca null si quieres evitar NPE)
            PagoResponseDTO errorDto = new PagoResponseDTO();
            errorDto.setStatus("ERROR");
            errorDto.setMessage("Error consultando pago: " + e.getMessage());

            return errorDto;
        }

    }

    public PagoResponseDTO mapResponse(String responseJson) {

        JsonObject json = Json.createReader(new StringReader(responseJson)).readObject();

        PagoResponseDTO dto = new PagoResponseDTO();

        // Status raíz
        JsonObject status = json.getJsonObject("status");

        dto.setStatus(status.getString("status", null));
        dto.setReason(status.getString("reason", null));
        dto.setMessage(status.getString("message", null));
        dto.setDate(status.getString("date", null));

        // Payment[0].authorization
        JsonArray payments = null;
        if (json.containsKey("payment") && json.get("payment").getValueType() == JsonValue.ValueType.ARRAY) {
            payments = json.getJsonArray("payment");
        }
//        JsonArray payments = json.getJsonArray("payment");

        if (payments != null && !payments.isEmpty()) {
            JsonObject payment0 = payments.getJsonObject(0);
            dto.setAuthorization(payment0.getString("authorization", null));
            dto.setNombreTarjeta(payment0.getString("paymentMethodName", null));
            dto.setReference(payment0.getString("reference", null));
        } else{
            System.out.println("Ingresa a rechazado o pendiente");
            JsonObject request = json.getJsonObject("request");
            if (request != null) {
                JsonObject paymentRequest = request.getJsonObject("payment");

                if (paymentRequest != null) {
                    String reference = paymentRequest.getString("reference", null);
                    dto.setAuthorization(null);
                    dto.setNombreTarjeta(null);
                    dto.setReference(reference);
                }
            }

            LOGGER.warning("No existe información de pago (payment es null o vacío)");
        }

        return dto;
    }


}
