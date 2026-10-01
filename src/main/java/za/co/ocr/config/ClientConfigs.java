package za.co.ocr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

@Configuration
public class ClientConfigs {
    // All DynamoDB calls use the same AWS region unless a profile changes the endpoint behavior.
    private static final Logger logger = LoggerFactory.getLogger(ClientConfigs.class);
    private static final Region REGION = Region.AF_SOUTH_1;
    // Used only as a fallback if the local profile does not provide an endpoint explicitly.
    private static final String DEFAULT_LOCALSTACK_ENDPOINT = "http://localhost:4566";

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(@Value("${aws.endpoint:}") String endpoint,
                                                         @Value("${aws.credentials.profile-name:}") String profileName,
                                                         @Value("${aws.credentials.access-key:}") String accessKey,
                                                         @Value("${aws.credentials.secret-key:}") String secretKey) {
        // Start with the common client builder and then add just the pieces needed for each mode.
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(REGION);

        if (hasText(accessKey) && hasText(secretKey)) {
            // LocalStack mode uses fake credentials and a local endpoint.
            String resolvedEndpoint = hasText(endpoint) ? endpoint : DEFAULT_LOCALSTACK_ENDPOINT;
            logger.info("Creating DynamoDB client for LocalStack at {}", resolvedEndpoint);
            builder.endpointOverride(URI.create(resolvedEndpoint));
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else if (hasText(profileName)) {
            // The local profile uses your AWS CLI named profile.
            logger.info("Creating DynamoDB client with AWS profile '{}'", profileName);
            builder.credentialsProvider(ProfileCredentialsProvider.builder().profileName(profileName).build());
        } else {
            // In real AWS Lambda, the runtime provides credentials automatically.
            logger.info("Creating DynamoDB client with default credentials chain");
        }

        // Wrap the low-level client in the enhanced client used by the repository code.
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(builder.build())
                .build();
    }

    private boolean hasText(String value) {
        // Small helper: returns true only when the string has at least one non-space character.
        return value != null && !value.isBlank();
    }
}
