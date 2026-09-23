package za.co.ocr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Stores the essential email metadata that we persist in DynamoDB.
 *
 * <p>Imagine this class as the small, structured summary of an email. It holds
 * who received the message, who sent it, the subject line, and when the email
 * arrived. The DynamoDB annotations tell the SDK which fields should be used as
 * the table's primary key so records can be saved and looked up efficiently.</p>
 */
@Data
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class EmailInfo {
    private String emailReceiver;
    private String emailSender;
    private String emailSubject;
    private String emailReceivedOn;

    @DynamoDbPartitionKey
    public String getEmailReceiver() {
        return emailReceiver;
    }

    @DynamoDbSortKey
    public String getEmailReceivedOn() {
        return emailReceivedOn;
    }

}
