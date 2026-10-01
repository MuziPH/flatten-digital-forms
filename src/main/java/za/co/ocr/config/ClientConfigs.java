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
    private static final Logger logger = LoggerFactory.getLogger(ClientConfigs.class);
    private static final String DEFAULT_LOCALSTACK_ENDPOINT = "http://localhost:4566";

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(@Value("${aws.region:af-south-1}") String region,
                                                         @Value("${aws.endpoint:}") String endpoint,
                                                         @Value("${aws.credentials.profile-name:}") String profileName,
                                                         @Value("${aws.credentials.access-key:}") String accessKey,
                                                         @Value("${aws.credentials.secret-key:}") String secretKey) {
        Region awsRegion = Region.of(region);
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(awsRegion);

        logConfiguration(region, endpoint, profileName, accessKey, secretKey);

        if (hasText(accessKey) && hasText(secretKey)) {
            configureForLocalStack(builder, endpoint, accessKey, secretKey);
        } else if (hasText(profileName)) {
            configureForAwsProfile(builder, profileName);
        } else {
            logDefaultAwsMode();
        }

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(builder.build())
                .build();
    }

    private void logConfiguration(String region,
                                  String endpoint,
                                  String profileName,
                                  String accessKey,
                                  String secretKey) {
        logger.info("=== DynamoDB Client Configuration ===");
        logger.info("region: {}", region);
        logger.info("endpoint configured: {}", hasText(endpoint));
        logger.info("profile configured: {}", hasText(profileName));
        logger.info("static access key configured: {}", hasText(accessKey));
        logger.info("static secret key configured: {}", hasText(secretKey));
    }

    private void configureForLocalStack(DynamoDbClientBuilder builder,
                                        String endpoint,
                                        String accessKey,
                                        String secretKey) {
        String resolvedEndpoint = hasText(endpoint) ? endpoint : DEFAULT_LOCALSTACK_ENDPOINT;
        logger.info(">>> Mode: LocalStack (static credentials + local endpoint)");
        logger.info(">>> Endpoint: {}", resolvedEndpoint);
        builder.endpointOverride(URI.create(resolvedEndpoint));
        builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
    }

    private void configureForAwsProfile(DynamoDbClientBuilder builder, String profileName) {
        logger.info(">>> Mode: Local AWS CLI Profile (named profile: '{}')", profileName);
        builder.credentialsProvider(ProfileCredentialsProvider.builder()
                .profileName(profileName)
                .build());
    }

    private void logDefaultAwsMode() {
        logger.info(">>> Mode: AWS default credentials chain");
        logger.info(">>> Expected for deployed Lambda where credentials come from the execution role");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
