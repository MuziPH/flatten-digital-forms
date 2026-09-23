package za.co.ocr.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

/**
 * Bootstraps the DynamoDB table used by the application when local table
 * creation is enabled.
 *
 * <p>In simple terms, this class checks whether the {@code EmailInfo} table
 * exists, verifies that its key schema matches what the code expects, and
 * recreates it when running in a development setup that allows automatic
 * provisioning. This keeps local experimentation predictable and saves you
 * from creating the table by hand every time.</p>
 */
@Configuration
@Slf4j
public class DynamoDbTableInitializer {
    private static final String TABLE_NAME = "EmailInfo";
    private static final String PARTITION_KEY = "emailReceiver";
    private static final String SORT_KEY = "emailReceivedOn";

    @Bean
    public ApplicationRunner emailInfoTableInitializer(DynamoDbClient dynamoDbClient,
                                                       @Value("${aws.dynamodb.auto-create-table:false}") boolean autoCreateTable,
                                                       @Value("${aws.dynamodb.endpoint:}") String dynamoDbEndpoint) {
        return args -> {
            if (!autoCreateTable) {
                log.info("DynamoDB table auto-creation disabled; expecting table {} to already exist", TABLE_NAME);
                return;
            }

            try {
                TableDescription table = dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build()).table();
                if (hasExpectedSchema(table)) {
                    log.info("DynamoDB table {} already exists at {} with the expected schema", TABLE_NAME, dynamoDbEndpoint);
                    return;
                }

                log.warn("DynamoDB table {} at {} has an incompatible schema; recreating it for local testing", TABLE_NAME, dynamoDbEndpoint);
                dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
                waitForTableDeletion(dynamoDbClient);
                createEmailInfoTable(dynamoDbClient, dynamoDbEndpoint);
            } catch (ResourceNotFoundException ex) {
                createEmailInfoTable(dynamoDbClient, dynamoDbEndpoint);
            }
        };
    }

    private boolean hasExpectedSchema(TableDescription table) {
        if (table.keySchema() == null || table.keySchema().size() != 2) {
            return false;
        }

        boolean hasExpectedPartitionKey = table.keySchema().stream()
                .anyMatch(key -> PARTITION_KEY.equals(key.attributeName()) && KeyType.HASH.equals(key.keyType()));
        boolean hasExpectedSortKey = table.keySchema().stream()
                .anyMatch(key -> SORT_KEY.equals(key.attributeName()) && KeyType.RANGE.equals(key.keyType()));

        return hasExpectedPartitionKey && hasExpectedSortKey;
    }

    private void waitForTableDeletion(DynamoDbClient dynamoDbClient) throws InterruptedException {
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(TABLE_NAME).build());
                Thread.sleep(500);
            } catch (ResourceNotFoundException ex) {
                return;
            }
        }

        throw new IllegalStateException("Timed out waiting for DynamoDB table " + TABLE_NAME + " to be deleted");
    }

    private void createEmailInfoTable(DynamoDbClient dynamoDbClient, String dynamoDbEndpoint) {
        log.info("Creating DynamoDB table {} at {}", TABLE_NAME, dynamoDbEndpoint);
        dynamoDbClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(PARTITION_KEY)
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName(SORT_KEY)
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(PARTITION_KEY)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(SORT_KEY)
                                .keyType(KeyType.RANGE)
                                .build())
                .build());
        log.info("Created DynamoDB table {} successfully", TABLE_NAME);
    }
}

