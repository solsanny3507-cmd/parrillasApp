package ec.gob.epmmop.parrillasapp.placetopay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlacetoPayResponse {

    private Status status;
    private String requestId;
    private String processUrl;
    private String reference;
    private String description;

    @Getter
    @Setter
    public static class Status {
        private String status;
        private String reason;
        private String message;
        private String date;
    }
}
