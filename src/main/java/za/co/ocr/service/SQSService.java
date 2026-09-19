package za.co.ocr.service;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import za.co.ocr.model.EmailInfo;

@Service
@Slf4j
public class SQSService {
    private static final String sqsQueueName = "cibwa-ocr-sqs-emailInfo-afs1-dev";
    private final SqsTemplate sqsTemplate;

    public SQSService(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    public void sendToSQS(EmailInfo emailInfo) {
        try {
            sqsTemplate.send(sqsQueueName, MessageBuilder.withPayload(emailInfo).build());

        } catch (RuntimeException e) {
            log.error("Failed to send email notification", e);

        }
    }

}
