package ec.gob.epmmop.parrillasapp.placetopay.service;

import ec.gob.epmmop.parrillasapp.general.repository.CiudadanoRepository;
import ec.gob.epmmop.parrillasapp.general.repository.ParametrosRepository;
import ec.gob.epmmop.parrillasapp.general.util.Utilitarios;
import ec.gob.epmmop.parrillasapp.parrillas.dto.DatosReservaDTO;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PaymentRequest;
import ec.gob.epmmop.parrillasapp.placetopay.dto.PlacetoPayRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class PaymentService extends Utilitarios {

    @Inject
    private CiudadanoRepository ciudadanoRepository;

    @Inject
    private ParametrosRepository parametrosRepository;

    private static final BigDecimal IVA_RATE = new BigDecimal("0.15");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public PlacetoPayRequest construirJsonSessionRequest(PaymentRequest request, DatosReservaDTO datosReserva) {

        var parametros = parametrosRepository.findByPrmSecuencial(1);

        BigDecimal precio = datosReserva.getPrecioTotal();
        BigDecimal baseImponible = datosReserva.getBaseImponible();
        BigDecimal iva = datosReserva.getValorIVA();
        String codigoReferencia = generarReferencia(request.getCodigoServicio());
        String seed = OffsetDateTime.now(ZoneOffset.of("-05:00")).toString();
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Map<String, String> nombres = separarNombreApellido(request.getCiudadano().getCiuNombre());

        String secretKey = parametros.getPrmSecretKey();
        String tranKey = generarTranKey(nonce, seed, secretKey);

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("login", parametros.getPrmLogin());
        auth.put("tranKey", tranKey);
        auth.put("nonce", Base64.getEncoder().encodeToString(nonce.getBytes(StandardCharsets.UTF_8)));
        auth.put("seed", seed);

        // taxes
        Map<String, Object> tax = Map.of(
                "kind", "valueAddedTax",
                "base", baseImponible,
                "amount", iva
        );

        PlacetoPayRequest.PaymentData paymentData = new PlacetoPayRequest.PaymentData();
        paymentData.setReference(codigoReferencia);
        paymentData.setDescription(request.getNombreServicio());

        PlacetoPayRequest.Amount amount = new PlacetoPayRequest.Amount();
        amount.setTaxes(List.of(tax));
        amount.setCurrency("USD");
        amount.setTotal(precio);

        paymentData.setAmount(amount);

        PlacetoPayRequest.Buyer buyer = new PlacetoPayRequest.Buyer();
        buyer.setName(nombres.get("nombre"));
        buyer.setSurname(nombres.get("apellido"));
        buyer.setDocument(request.getCiudadano().getCiuIdentificacion());
        buyer.setEmail(request.getCiudadano().getCiuCorreo());
        buyer.setDocumentType("CI");
        buyer.setMobile(request.getCiudadano().getCiuTelefono());

        PlacetoPayRequest placetoPayRequest = new PlacetoPayRequest();
        placetoPayRequest.setLocale("es_EC");
        placetoPayRequest.setAuth(auth);
        placetoPayRequest.setPayment(paymentData);
        placetoPayRequest.setBuyer(buyer);
        placetoPayRequest.setExpiration(LocalDateTime.now().plusMinutes(15).toString());
        placetoPayRequest.setReturnUrl(parametros.getPrmUrlReturn());
        placetoPayRequest.setCancelUrl(parametros.getPrmUrlCancel());
        placetoPayRequest.setIpAddress(request.getIp());
        placetoPayRequest.setUserAgent("PlacetoPay Sandbox");

        return placetoPayRequest;
    }

    public Map<String, Object> buildPlacetoPayVerificationRequest(Integer request) {
        var parametros = parametrosRepository.findByPrmSecuencial(1);

        String seed = OffsetDateTime.now(ZoneOffset.of("-05:00")).toString();
        String noncePlain = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String nonce = Base64.getEncoder().encodeToString(noncePlain.getBytes(StandardCharsets.UTF_8));

        String tranKey = generarTranKey(noncePlain, seed, parametros.getPrmSecretKey());

        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("login", parametros.getPrmLogin());
        auth.put("tranKey", tranKey);
        auth.put("nonce", nonce);
        auth.put("seed", seed);

        return Map.of("auth", auth);
    }

    public String generarReferencia(String servicio) {
        String fecha = LocalDateTime.now().format(FORMATTER);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return servicio + "-" + fecha + "-" + random;
    }

    private Map<String, String> separarNombreApellido(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+");

        List<String> conectores = List.of("de", "del", "la", "las", "los");

        List<String> apellidos = new ArrayList<>();
        List<String> nombres = new ArrayList<>();

        int i = 0;

        while (i < partes.length && apellidos.size() < 2) {
            String actual = partes[i];

            // Si es conector, se une al apellido anterior
            if (!apellidos.isEmpty() && conectores.contains(actual.toLowerCase())) {
                // Unir con el siguiente también
                if (i + 1 < partes.length) {
                    String compuesto = apellidos.remove(apellidos.size() - 1)
                            + " " + actual + " " + partes[i + 1];
                    apellidos.add(compuesto);
                    i += 2;
                } else {
                    apellidos.add(actual);
                    i++;
                }
            } else {
                apellidos.add(actual);
                i++;
            }
        }

        // El resto son nombres
        while (i < partes.length) {
            nombres.add(partes[i]);
            i++;
        }

        return Map.of(
                "nombre", String.join(" ", nombres),
                "apellido", String.join(" ", apellidos)
        );
    }

    private String generarTranKey(String nonce, String seed, String secretKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String toHash = nonce + seed + secretKey;
            byte[] hash = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generando tranKey", e);
        }
    }

}
