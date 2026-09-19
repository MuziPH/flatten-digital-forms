package za.co.ocr.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import za.co.ocr.model.EmailInfo;

@Repository
@Slf4j
public class EmailInfoRepository {
    private static final String tableName = "EmailInfo";
    private final DynamoDbTable<EmailInfo> emailInfoTable;

    public EmailInfoRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.emailInfoTable = dynamoDbEnhancedClient.table(tableName,
                TableSchema.fromBean(EmailInfo.class)
        );
    }

    // Save
    public void saveEmailInfo(EmailInfo emailInfo) {
        log.info("saving email info {}", emailInfo);
        try {
            emailInfoTable.putItem(emailInfo);
        } catch (RuntimeException e) {
            log.error("Error saving email info to DynamoDB: {}", e.getMessage());
        }
    }
}