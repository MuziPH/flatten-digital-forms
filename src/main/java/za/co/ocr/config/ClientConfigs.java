package za.co.ocr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class ClientConfigs {
    private static final Logger logger = LoggerFactory.getLogger(ClientConfigs.class);
    private static final Region REGION = Region.AF_SOUTH_1;

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        logger.info("Creating DynamoDB client with default credentials (Lambda role)");
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.builder().region(REGION).build())
                .build();
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        logger.info("Creating SQS client with default credentials (Lambda role)");
        return SqsAsyncClient.builder().region(REGION).build();
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.newTemplate(sqsAsyncClient);
    }
}
