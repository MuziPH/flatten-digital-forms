package za.co.ocr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class ClientConfigs {
    private static final Region REGION = Region.AF_SOUTH_1;

    @Configuration
    @Profile("localstack")
    static class LocalstackConfig {
        private static final Logger logger = LoggerFactory.getLogger(LocalstackConfig.class);

        @Value("${aws.endpoint}")
        private String endpoint;

        @Value("${aws.credentials.access-key}")
        private String accessKey;

        @Value("${aws.credentials.secret-key}")
        private String secretKey;

        private StaticCredentialsProvider credentialsProvider() {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }

        @Bean
        public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
            logger.info("Creating DynamoDB client for LocalStack at {}", endpoint);
            return DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(DynamoDbClient.builder()
                            .region(REGION)
                            .endpointOverride(URI.create(endpoint))
                            .credentialsProvider(credentialsProvider())
                            .build())
                    .build();
        }
    }

    @Configuration
    @Profile("local")
    static class LocalConfig {
        private static final Logger logger = LoggerFactory.getLogger(LocalConfig.class);

        @Value("${aws.credentials.profile-name}")
        private String profileName;

        @Bean
        public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
            logger.info("Creating DynamoDB client with AWS profile '{}'", profileName);
            return DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(DynamoDbClient.builder()
                            .region(REGION)
                            .credentialsProvider(ProfileCredentialsProvider.builder().profileName(profileName).build())
                            .build())
                    .build();
        }
    }

    @Configuration
    @Profile("aws")
    static class AwsConfig {
        private static final Logger logger = LoggerFactory.getLogger(AwsConfig.class);

        @Bean
        public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
            logger.info("Creating DynamoDB client with default credentials (Lambda role)");
            return DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(DynamoDbClient.builder().region(REGION).build())
                    .build();
        }
    }
}
