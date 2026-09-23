package za.co.ocr.model;

import lombok.Data;

import java.util.List;

/**
 * Describes the full email instruction payload that is nested inside the SQS
 * message body.
 *
 * <p>This object contains the human-readable parts of the email plus the list
 * of attachments that still need to be processed. The application uses this
 * model to turn the raw JSON body into Java fields it can work with safely.</p>
 */
@Data
public class OTTEmailInstruction {
    private String sender;
    private String from;
    private String subject;
    private String receivedDateTime;
    private List<Attachment> emailAttachments;
    private String messageId;

    /**
     * Represents one attachment referenced by the email instruction.
     */
    @Data
    public static class Attachment {
        private String attachmentName;
        private String attachmentS3Key;
        private Integer attachmentSize;
        private String contentType;
    }
}
