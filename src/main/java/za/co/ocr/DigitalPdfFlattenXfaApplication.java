package za.co.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.LambdaEvent;
import za.co.ocr.model.OTTEmailInstruction;
import za.co.ocr.repository.EmailInfoRepository;

import java.util.function.Consumer;

/**
 * Main Spring Boot application for the PDF flattening workflow.
 *
 * <p>Think of this class as the starting point for the whole application:
 * Spring Boot launches from here, wires together the repository and AWS
 * clients, and exposes the {@code flattenPDF()} function that processes
 * Lambda-style SQS event payloads.</p>
 */
@SpringBootApplication
@Slf4j
public class DigitalPdfFlattenXfaApplication {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final EmailInfoRepository emailInfoRepository;

    public DigitalPdfFlattenXfaApplication(EmailInfoRepository emailInfoRepository) {
        this.emailInfoRepository = emailInfoRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(DigitalPdfFlattenXfaApplication.class, args);
    }

    /**
     * Lambda entry point used when AWS invokes the application with an SQS event.
     */
    @Bean
    public Consumer<String> flattenPDF() {
        return input -> {
            log.info("=== Lambda function invoked ===");
            log.info("Received input: {}", input);

            try {
                // SQS delivers a JSON document with one or more Records.
                // Each record represents one message that came off the queue.
                LambdaEvent lambdaEvent = objectMapper.readValue(input, LambdaEvent.class);
                log.info("Parsed LambdaEvent with {} record(s)", lambdaEvent.getRecords().size());

                // Process each SQS message one by one.
                for (LambdaEvent.Record sqsRecord : lambdaEvent.getRecords()) {
                    log.info("Processing record with messageId: {}", sqsRecord.getMessageId());

                    // The SQS body is a JSON string that contains the email details.
                    processInstructionJson(sqsRecord.getBody());
                }

            } catch (Exception e) {
                log.error("Error processing message", e);
            }

            log.info("=== Lambda function completed ===");
        };
    }

    private void processInstructionJson(String instructionJson) throws Exception {
        log.info("Instruction JSON: {}", instructionJson);

        OTTEmailInstruction instruction = objectMapper.readValue(instructionJson, OTTEmailInstruction.class);
        EmailInfo emailInfo = toEmailInfo(instruction);
        int attachmentCount = instruction.getEmailAttachments() == null ? 0 : instruction.getEmailAttachments().size();

        log.info("Parsed EmailInfo: sender={}, subject={}, attachments={}",
                instruction.getSender(),
                instruction.getSubject(),
                attachmentCount);

        if (attachmentCount == 0) {
            log.warn("No attachments found in email from {}", instruction.getSender());
            emailInfoRepository.saveEmailInfo(emailInfo);
            return;
        }

        for (OTTEmailInstruction.Attachment attachment : instruction.getEmailAttachments()) {
            log.info("Processing attachment: {} ({})",
                    attachment.getAttachmentName(),
                    attachment.getAttachmentS3Key());
        }
    }

    private EmailInfo toEmailInfo(OTTEmailInstruction instruction) {
        return new EmailInfo(
                instruction.getFrom(),
                instruction.getSender(),
                instruction.getSubject(),
                instruction.getReceivedDateTime()
        );
    }


}
