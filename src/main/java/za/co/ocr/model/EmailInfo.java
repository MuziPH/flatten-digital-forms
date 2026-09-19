package za.co.ocr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class EmailInfo {
    private String emailReceiver;
    private String emailSender;
    private String emailSubject;
    private String emailReceivedOn;

}
