package za.co.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.LambdaEvent;
import za.co.ocr.model.OTTEmailInstruction;
import za.co.ocr.service.SQSService;

import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
public class DigitalPdfFlattenXfaApplication {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final SQSService sqsService;

    public DigitalPdfFlattenXfaApplication(SQSService sqsService) {
        this.sqsService = sqsService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DigitalPdfFlattenXfaApplication.class, args);
    }

    @Bean
    public Consumer<String> flattenPDF() {
        return input -> {
            log.info("=== Lambda function invoked ===");
            log.info("Received input: {}", input);

            try {
                // Parse the SQS event
                LambdaEvent lambdaEvent = objectMapper.readValue(input, LambdaEvent.class);
                log.info("Parsed LambdaEvent with {} record(s)", lambdaEvent.getRecords().size());

                // Process each record in the SQS message
                for (LambdaEvent.Record sqsRecord : lambdaEvent.getRecords()) {
                    log.info("Processing record with messageId: {}", sqsRecord.getMessageId());

                    // Extract the body (which is a JSON string) and parse it as EmailInfo
                    String bodyJson = sqsRecord.getBody();
                    log.info("Body JSON: {}", bodyJson);

                    OTTEmailInstruction ottEmailInstruction = objectMapper.readValue(bodyJson, OTTEmailInstruction.class);
                    EmailInfo emailInfo = new EmailInfo(
                            ottEmailInstruction.getFrom(),
                            ottEmailInstruction.getSender(),
                            ottEmailInstruction.getSubject(),
                            ottEmailInstruction.getReceivedDateTime()
                    );
                    log.info("Parsed EmailInfo: sender={}, subject={}, attachments={}", ottEmailInstruction.getSender(), ottEmailInstruction.getSubject(), ottEmailInstruction.getEmailAttachments() != null ? ottEmailInstruction.getEmailAttachments().size() : 0);

                    // Handle no attachments
                    if (ottEmailInstruction.getEmailAttachments() == null || ottEmailInstruction.getEmailAttachments().isEmpty()) {
                        log.warn("No attachments found in email from {}", ottEmailInstruction.getSender());
                        // skip classifying and extraction and send to sqs for payment or link doc carrier
                        log.info("Sending message to SQS queue for link doc carrier");
                        sqsService.sendToSQS(emailInfo);
                        // Send to SQS for to be picked up by the link doc carrier service
                        continue;
                    }

                    // Add your PDF flattening logic here
                    for (OTTEmailInstruction.Attachment attachment : ottEmailInstruction.getEmailAttachments()) {
                        log.info("Processing attachment: {} ({})", attachment.getAttachmentName(), attachment.getAttachmentS3Key());
                        // TODO: Implement PDF flattening logic
                    }
                } // End processing all lambda events

            } catch (Exception e) {
                log.error("Error processing message", e);
            }

            log.info("=== Lambda function completed ===");
        };
    }

}
