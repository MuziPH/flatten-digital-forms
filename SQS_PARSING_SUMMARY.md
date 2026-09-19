# SQS Message Parsing - Implementation Summary

## Changes Made

### 1. Updated `LambdaEvent.java` Model
**File:** `src/main/java/za/co/ocr/model/LambdaEvent.java`

**Changes:**
- Added `@JsonProperty("Records")` annotation to explicitly map the JSON field name (AWS SQS uses capital "R")
- The field name is `Records` (capital R) to match AWS SQS JSON format
- Updated `Record.body` from `OTTEmailInstruction` object to `String` (as SQS body is a JSON string)
- Added complete SQS record fields:
  - `messageId`
  - `receiptHandle`
  - `body` (String - contains JSON)
  - `attributes`
  - `messageAttributes`
  - `md5OfMessageAttributes`
  - `md5OfBody`
  - `eventSource`
  - `eventSourceARN`
  - `awsRegion`
- Added `MessageAttribute` inner class for message attributes structure

**Important Note:** The `@JsonProperty("Records")` annotation is crucial because AWS SQS messages use capital "R" in "Records", and Jackson needs this explicit mapping when using Lombok's `@Data` annotation.

### 2. Updated Lambda Function Logic
**File:** `src/main/java/za/co/ocr/DigitalPdfFlattenXfaApplication.java`

**Changes:**
- The function now correctly parses the SQS event structure:
  1. Parse the entire input as `LambdaEvent`
  2. Iterate through each `Record` in the `Records` array
  3. Extract the `body` field (which is a JSON string)
  4. Parse the `body` string as `OTTEmailInstruction` object
  5. Process the EmailInfo and its attachments

### 3. Added Test
**File:** `src/test/java/za/co/ocr/SqsMessageParsingTest.java`

**Purpose:** Validates that the SQS message parsing works correctly with the actual message format provided.

## How It Works

Given the SQS message structure:
```json
{
  "Records": [
    {
      "messageId": "...",
      "body": "{\"sender\":\"...\", \"subject\":\"...\", ...}",
      ...
    }
  ]
}
```

The parsing flow is:
1. **Input String** → Parse as → **LambdaEvent** object
2. **LambdaEvent.Records[0].body** → Extract as → **String (JSON)**
3. **Body String** → Parse as → **EmailInfo** object

## Key Points

- **Two-stage parsing**: First parse the SQS event wrapper, then parse the body content
- **Body is a string**: The SQS `body` field contains a JSON string (not a direct object), which requires a second JSON parsing step
- **Field naming**: AWS SQS uses `"Records"` with capital R - the `@JsonProperty` annotation ensures proper mapping
- **Complete record structure**: The Record class now includes all relevant SQS fields for potential future use

## Issue Resolved

**Error Fixed:** 
```
com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: 
Unrecognized field "Records" (class za.co.ocr.model.LambdaEvent)
```

**Solution:** Added `@JsonProperty("Records")` annotation to explicitly map the JSON field name from AWS SQS (which uses capital "R") to the Java field.

## Testing

The implementation has been tested with the actual SQS message format and successfully:
- ✅ Parses the SQS event structure with "Records" field
- ✅ Extracts the body JSON string
- ✅ Parses EmailInfo with all fields including attachments
- ✅ Validates all data integrity
- ✅ Build successful with packaged JAR ready for deployment

## Next Steps

The TODO comment remains in the code for implementing the actual PDF flattening logic:
```java
// TODO: Implement PDF flattening logic
```

This should be implemented to process each PDF attachment using the S3 key.

