package za.co.ocr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Creates the AWS clients used by the application.
 *
 * <p>This class centralizes how we build DynamoDB clients so the rest of the
 * code can just ask Spring for a ready-to-use client. It also supports a local
 * endpoint override, which is handy when running against DynamoDB Local during
 * development or tests.</p>
 */
@Configuration
public class ClientConfigs {
    private static final Logger logger = LoggerFactory.getLogger(ClientConfigs.class);
    private static final Region REGION = Region.AF_SOUTH_1;
    private final String dynamoDbEndpoint;

    public ClientConfigs(@Value("${aws.dynamodb.endpoint:}") String dynamoDbEndpoint) {
        this.dynamoDbEndpoint = dynamoDbEndpoint;
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        logger.info("Creating DynamoDB client for region {}{}", REGION,
                hasText(dynamoDbEndpoint) ? " using endpoint override " + dynamoDbEndpoint : " with default AWS endpoint");
        return configureAwsClient(DynamoDbClient.builder(), dynamoDbEndpoint).build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    private <T extends AwsClientBuilder<T, ?>> T configureAwsClient(T builder, String endpoint) {
        builder.region(REGION);

        if (hasText(endpoint)) {
            return builder
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(AnonymousCredentialsProvider.create());
        }

        return builder.credentialsProvider(DefaultCredentialsProvider.create());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
