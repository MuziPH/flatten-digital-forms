# Fix for UnrecognizedPropertyException: SQS Message Parsing

## Problems Fixed

### 1. "Records" Field Error
The Lambda function was throwing an error when processing SQS messages:
```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: 
Unrecognized field "Records" (class za.co.ocr.model.LambdaEvent), 
not marked as ignorable (one known property: "records")
```

### 2. "stringListValues" Field Error
After fixing the Records issue, another error appeared:
```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: 
Unrecognized field "stringListValues" (class za.co.ocr.model.LambdaEvent$MessageAttribute), 
not marked as ignorable (2 known properties: "stringValue", "dataType")
```

## Root Causes

### Issue 1: Records Field
AWS SQS messages use `"Records"` with a capital "R" in the JSON, but the Java field was initially defined as `records` (lowercase). When using Lombok's `@Data` annotation with Jackson, the field name needs to match the JSON field name exactly, or use `@JsonProperty` annotation to specify the mapping.

### Issue 2: MessageAttribute Fields
The `MessageAttribute` class was missing the `stringListValues` and `binaryListValues` fields that are present in the actual AWS SQS message structure.

## Solutions Applied

### 1. Added @JsonProperty annotation for Records field
```java
@Data
public class LambdaEvent {
    @JsonProperty("Records")
    private List<Record> Records;
    // ... rest of the class
}
```

### 2. Added missing fields to MessageAttribute class
```java
@Data
public static class MessageAttribute {
    private String stringValue;
    private List<String> stringListValues;
    private List<String> binaryListValues;
    private String dataType;
}
```

This explicitly tells Jackson to:
- Map the JSON field `"Records"` (capital R) from AWS SQS to the Java field
- Parse the additional list fields in message attributes

## Files Modified
1. **LambdaEvent.java** 
   - Added `@JsonProperty("Records")` annotation
   - Added `stringListValues` and `binaryListValues` fields to `MessageAttribute` class
2. **SqsMessageParsingTest.java** - Updated test data to use `"Records"` (capital R)
3. **SQS_PARSING_SUMMARY.md** - Updated documentation

## Verification
✅ All tests pass (2/2)
✅ Build successful
✅ Package created: `digital-pdf-flatten-xfa-0.0.1-SNAPSHOT-aws.jar`
✅ Ready for AWS Lambda deployment

## AWS SQS Message Format
The Lambda function now correctly handles AWS SQS messages with this complete structure:
```json
{
  "Records": [
    {
      "messageId": "...",
      "receiptHandle": "...",
      "body": "{\"sender\":\"...\",\"subject\":\"...\", ...}",
      "attributes": {...},
      "messageAttributes": {
        "JavaType": {
          "stringValue": "za.co.cims.ocr.emailNotificationHandler.model.EmailInfo",
          "stringListValues": [],
          "binaryListValues": [],
          "dataType": "String"
        },
        "contentType": {
          "stringValue": "application/json",
          "stringListValues": [],
          "binaryListValues": [],
          "dataType": "String"
        }
      },
      ...
    }
  ]
}
```

## Test Results
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Both tests pass:
- ✅ `DigitalPdfFlattenXfaApplicationTests` - Spring Boot context loads correctly
- ✅ `SqsMessageParsingTest` - SQS message parsing with complete structure works correctly

