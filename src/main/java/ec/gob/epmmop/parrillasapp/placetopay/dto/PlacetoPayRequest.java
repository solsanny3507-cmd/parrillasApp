package ec.gob.epmmop.parrillasapp.placetopay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PlacetoPayRequest {

    private String locale;
    private Map<String, Object> auth;
    private PaymentData payment;
    private Buyer buyer;
    private String expiration;
    private String returnUrl;
    private String cancelUrl;
    private String ipAddress;
    private String userAgent;

    @Data
    public static class PaymentData {
        private String reference;
        private String description;
        private Amount amount;
    }

    @Data
    public static class Amount {
        private List<Map<String, Object>> taxes;
        private String currency;
        private BigDecimal total;
    }

    @Data
    public static class Buyer {
        private String name;
        private String surname;
        private String document;
        private String email;
        private String documentType;
        private String mobile;
    }
}
