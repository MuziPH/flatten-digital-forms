package za.co.ocr.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Models the JSON payload that AWS Lambda receives from SQS.
 *
 * <p>The top-level event contains a list of records because one Lambda
 * invocation may deliver multiple queue messages at once. Each nested record
 * represents one SQS message, and the nested message-attribute class mirrors
 * the shape of the JSON that AWS sends.</p>
 */
@Data
public class LambdaEvent {
    @JsonProperty("Records")
    private List<Record> Records;

    /**
     * Represents one SQS message inside the Lambda event payload.
     */
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

    /**
     * Represents one custom message attribute attached to an SQS message.
     */
    @Data
    public static class MessageAttribute {
        private String stringValue;
        private List<String> stringListValues;
        private List<String> binaryListValues;
        private String dataType;
    }
}
