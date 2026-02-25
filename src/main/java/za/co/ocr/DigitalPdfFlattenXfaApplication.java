package za.co.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import za.co.ocr.model.EmailInfo;
import za.co.ocr.model.LambdaEvent;

import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
public class DigitalPdfFlattenXfaApplication {
	private static final ObjectMapper objectMapper = new ObjectMapper();

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

					EmailInfo emailInfo = objectMapper.readValue(bodyJson, EmailInfo.class);
					log.info("Parsed EmailInfo: sender={}, subject={}, attachments={}",
							emailInfo.getSender(),
							emailInfo.getSubject(),
							emailInfo.getEmailAttachments() != null ? emailInfo.getEmailAttachments().size() : 0);

					// Add your PDF flattening logic here
					if (emailInfo.getEmailAttachments() != null) {
						for (EmailInfo.Attachment attachment : emailInfo.getEmailAttachments()) {
							log.info("Processing attachment: {} ({})",
									attachment.getAttachmentName(),
									attachment.getAttachmentS3Key());
							// TODO: Implement PDF flattening logic
						}
					}
				}

			} catch (Exception e) {
				log.error("Error processing message", e);
			}
			
			log.info("=== Lambda function completed ===");
		};
	}

}
