package za.co.ocr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LambdaEvent {
    @JsonProperty("Records")
    private List<Record> Records;

    @Data
    public static class Record {
        private String messageId;
        private String receiptHandle;
        private String body;
        private Map<String, String> attributes;
        private Map<String, MessageAttribute> messageAttributes;
        private String md5OfMessageAttributes;
        private String md5OfBody;
        private String eventSource;
        private String eventSourceARN;
        private String awsRegion;
    }

    @Data
    public static class MessageAttribute {
        private String stringValue;
        private List<String> stringListValues;
        private List<String> binaryListValues;
        private String dataType;
    }
}
