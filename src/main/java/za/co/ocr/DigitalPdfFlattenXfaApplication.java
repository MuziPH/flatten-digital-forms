package za.co.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import za.co.ocr.dto.CreatePaymentTransactionResponse;
import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.LambdaEvent;
import za.co.ocr.model.OTTEmailInstruction;
import za.co.ocr.repository.EmailInfoRepository;
import za.co.ocr.service.LinkDocCarrierService;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Main Spring Boot application for the PDF flattening Lambda.
 *
 * <p>Think of this class as the starting point for the whole application:
 * Spring Boot launches from here, wires together the repository and AWS
 * clients, and exposes the {@code flattenPDF()} function that Lambda calls
 * whenever an event arrives.</p>
 */
@SpringBootApplication
@Slf4j
public class DigitalPdfFlattenXfaApplication {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final EmailInfoRepository emailInfoRepository;
    private final LinkDocCarrierService linkDocCarrierService;

    public DigitalPdfFlattenXfaApplication(EmailInfoRepository emailInfoRepository, LinkDocCarrierService linkDocCarrierService) {
        this.emailInfoRepository = emailInfoRepository;
        this.linkDocCarrierService = linkDocCarrierService;
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
                // Parse the Lambda event payload
                LambdaEvent lambdaEvent = objectMapper.readValue(input, LambdaEvent.class);
                log.info("Parsed LambdaEvent with {} record(s)", lambdaEvent.getRecords().size());

                // Process each record in the event
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
                        // skip classifying and extraction and insert into DynamoDB EmailInfo table
                        emailInfoRepository.saveEmailInfo(emailInfo);
                        // send to BAW link doc carrier
                       // CreatePaymentTransactionResponse paymentTransaction = linkDocCarrierService.createPaymentTransaction(UUID.randomUUID().toString(), emailInfo);
                      //  log.info("send to BAW link doc carrier response: {}", paymentTransaction.getStatus());
                        // log workflow reference number
                     //   log.info("workflowReferenceNumber: {}", paymentTransaction.getData().getData().getWorkflowReferenceNumber());
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
