package za.co.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePaymentTransactionResponse {
    private String status;
    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String serviceStatus;
        private String key;
        private String step;
        private boolean reset;
        private InnerData data;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InnerData {
        private String response;
        private String workflowReferenceNumber;
    }
}
