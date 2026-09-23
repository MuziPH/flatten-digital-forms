package za.co.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import za.co.ocr.model.OTTEmailInstruction;
import za.co.ocr.model.LambdaEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates how the application maps raw SQS JSON into Java objects.
 *
 * <p>This test is useful for beginners because it shows the full journey from
 * a JSON event, to a {@link LambdaEvent}, and then to the nested email payload
 * inside the message body. If this parsing works, the Lambda handler can safely
 * focus on business logic instead of JSON handling.</p>
 */
public class SqsMessageParsingTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testParseSqsMessage() throws Exception {
        String sqsMessage = """
            {
                "Records": [
                    {
                        "messageId": "c295c9ec-39ae-40df-9900-310a951333c9",
                        "receiptHandle": "AQEBeNwscUibMj7IEIModlOT/K9+vFzh30vKd0HDugHAeatTtSxHq657nU6aopv2QeS1tAmwy2dHGtvPLO/7WXn49FtA0LKjbhDgfFp0E8dnm4DsmsVQlD7upxivmitHGWzScy/q3gfMrCJQKu7pE3qNnUjTtReXDVJ2dDcerOQBfosHW6LCSQMPdn5ZBWa1kMeBebOTdfIQD1F8RSthKftNWUFBRvNPfOITZ/bRoAr5b/nxlpIBsGp9qp/ZkZg1XGHTAtPj95qbL10oMLxzWAD5Na4kN5U+9zN2ktSj0iPo+rx9V9plfHHd/xWsjJ0IENFrL1d9HY7ucdCEZXVBO27CSP5h2OP3SyAOm6mJhxa2xdM0pilNel8Q+XH3SWoFAKRsspCY5Mtfm3AcKqMhAgKId8Ww+e6Ry2WQyjAwni63vEY=",
                        "body": "{\\"sender\\":\\"Muzi.Phage@standardbank.co.za\\",\\"from\\":\\"Muzi.Phage@standardbank.co.za\\",\\"subject\\":\\"Test Logos Icons\\",\\"receivedDateTime\\":\\"2026-02-25T10:30:59.303056300+02:00\\",\\"emailAttachments\\":[{\\"attachmentName\\":\\"ANGLOPLATINUM3.pdf\\",\\"attachmentS3Key\\":\\"AAMkADA4MDI4YTUwLTU5NjktNDYwMy1hYTQ3LWQ4YTcwMjlkMmIwOABGAAAAAACPiChSOIJCR5adSN4LHk0HBwDPLbbQ1ky3SZbw_joY2ty_AAAAAAEMAAByPg9CqrUQSZlGHeTTJJhcAAAujQ8MAAA=/attachments/digital/ANGLOPLATINUM3.pdf\\",\\"attachmentSize\\":147548,\\"contentType\\":\\"application/pdf\\"}],\\"messageId\\":\\"AAMkADA4MDI4YTUwLTU5NjktNDYwMy1hYTQ3LWQ4YTcwMjlkMmIwOABGAAAAAACPiChSOIJCR5adSN4LHk0HBwDPLbbQ1ky3SZbw_joY2ty_AAAAAAEMAAByPg9CqrUQSZlGHeTTJJhcAAAujQ8MAAA=\\"}",
                        "attributes": {
                            "ApproximateReceiveCount": "1",
                            "SentTimestamp": "1772008262205",
                            "SenderId": "AROASYS54W6CFQ7L6EEZG:ea153672@standardbank.onmicrosoft.com",
                            "ApproximateFirstReceiveTimestamp": "1772008262211"
                        },
                        "messageAttributes": {
                            "JavaType": {
                                "stringValue": "za.co.cims.ocr.emailNotificationHandler.model.EmailInfo",
                                "dataType": "String"
                            },
                            "contentType": {
                                "stringValue": "application/json",
                                "dataType": "String"
                            }
                        },
                        "md5OfMessageAttributes": "1e34675446beca6c3da114176e194193",
                        "md5OfBody": "54cd7ed12eae91075ed147c85cd85ce9",
                        "eventSource": "aws:sqs",
                        "eventSourceARN": "arn:aws:sqs:af-south-1:190249219972:cibwa-ocr-sqs-digital-afs1-dev",
                        "awsRegion": "af-south-1"
                    }
                ]
            }
            """;

        // Parse the SQS event
        LambdaEvent lambdaEvent = objectMapper.readValue(sqsMessage, LambdaEvent.class);

        assertNotNull(lambdaEvent);
        assertNotNull(lambdaEvent.getRecords());
        assertEquals(1, lambdaEvent.getRecords().size());

        LambdaEvent.Record record = lambdaEvent.getRecords().get(0);
        assertEquals("c295c9ec-39ae-40df-9900-310a951333c9", record.getMessageId());
        assertNotNull(record.getBody());

        // Parse the body as EmailInfo
        String bodyJson = record.getBody();
        OTTEmailInstruction ottEmailInstruction = objectMapper.readValue(bodyJson, OTTEmailInstruction.class);

        assertNotNull(ottEmailInstruction);
        assertEquals("Muzi.Phage@standardbank.co.za", ottEmailInstruction.getSender());
        assertEquals("Muzi.Phage@standardbank.co.za", ottEmailInstruction.getFrom());
        assertEquals("Test Logos Icons", ottEmailInstruction.getSubject());
        assertEquals("2026-02-25T10:30:59.303056300+02:00", ottEmailInstruction.getReceivedDateTime());

        assertNotNull(ottEmailInstruction.getEmailAttachments());
        assertEquals(1, ottEmailInstruction.getEmailAttachments().size());

        OTTEmailInstruction.Attachment attachment = ottEmailInstruction.getEmailAttachments().get(0);
        assertEquals("ANGLOPLATINUM3.pdf", attachment.getAttachmentName());
        assertEquals("AAMkADA4MDI4YTUwLTU5NjktNDYwMy1hYTQ3LWQ4YTcwMjlkMmIwOABGAAAAAACPiChSOIJCR5adSN4LHk0HBwDPLbbQ1ky3SZbw_joY2ty_AAAAAAEMAAByPg9CqrUQSZlGHeTTJJhcAAAujQ8MAAA=/attachments/digital/ANGLOPLATINUM3.pdf",
                attachment.getAttachmentS3Key());
        assertEquals(147548, attachment.getAttachmentSize());
        assertEquals("application/pdf", attachment.getContentType());
    }
}

