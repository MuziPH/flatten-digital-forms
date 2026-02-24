package za.co.ocr.model;

import lombok.Data;

import java.util.List;

@Data
public class EmailInfo {
    private String sender;
    private String from;
    private String subject;
    private String receivedDateTime;
    private List<Attachment> emailAttachments;
    private String messageId;

    @Data
    public static class Attachment {
        private String attachmentName;
        private String attachmentS3Key;
        private Integer attachmentSize;
        private String contentType;
    }
}
