# digital-pdf-flatten-xfa

This project processes SQS email events, extracts the email metadata needed by the workflow, and stores that metadata in DynamoDB. It is built with Spring Boot + Spring Cloud Function so the same codebase can run in three different ways:

- as a deployed AWS Lambda
- as a local servlet application that exposes the `flattenPDF` functional bean over HTTP while connected to real AWS resources
- as a local SAM function connected to LocalStack through Finch

## Quick Start

### Local development against real AWS

Start the local servlet app with the verified AWS CLI profile:

```bash
cd /Users/muzi.phage/sbsa-workspace/aws/cibwa-ocr-digital-pdf-flatten-xfa
AWS_PROFILE=muzi-mac-profile ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Send a test request to the function bean path:

```bash
curl -i -X POST http://localhost:8080/flattenPDF \
  -H 'Content-Type: application/json' \
  --data @events/sqs-dynamodb-insert-event.json
```

### Local SAM + LocalStack

```bash
cd /Users/muzi.phage/sbsa-workspace/aws/cibwa-ocr-digital-pdf-flatten-xfa
sam build --template template.yaml
sam local invoke FlattenXfaFunction \
  --env-vars events/env-vars.localstack.json \
  --event events/sqs-dynamodb-insert-event.json
```

### Deploy to AWS

Validate and deploy the SAM application:

```bash
cd /Users/muzi.phage/sbsa-workspace/aws/cibwa-ocr-digital-pdf-flatten-xfa
sam validate -t template.yaml --region af-south-1
sam build --template template.yaml
sam deploy --guided
```

## High-level architecture

At a high level, the application flow is:

1. An SQS message arrives
2. Spring Cloud Function routes the payload to `flattenPDF`
3. The application reads the SQS `Records[]`
4. Each record body is parsed into an `OTTEmailInstruction`
5. The relevant email metadata is mapped into `EmailInfo`
6. `EmailInfoRepository` writes the item to DynamoDB

When running with the `local` profile, that same processing flow can be exercised by posting the full Lambda-style SQS event payload to the `flattenPDF` functional bean over HTTP.

## Runtime modes

The project intentionally separates the three environments below.

### 1. `local` → local HTTP exposure of `flattenPDF` + real AWS

Use this when you want the easiest local development loop while still talking to real AWS services.

- Spring profile: `local`
- Web server: embedded Tomcat on port `8080`
- Credentials: AWS CLI named profile from `AWS_PROFILE`
- Default profile name: `muzi-mac-profile`
- DynamoDB behavior: expects the real `EmailInfo` table to already exist
- Local test endpoint: `POST /flattenPDF`

`src/main/resources/application-local.yaml` configures this mode.

### 2. `aws` → deployed Lambda on AWS

Use this when the function is deployed to AWS.

- Spring profile: `aws`
- Runtime: AWS Lambda
- Credentials: AWS default credential chain
- Expected credential source: Lambda execution role
- DynamoDB behavior: expects the real `EmailInfo` table to already exist

`template.yaml` sets `SPRING_PROFILES_ACTIVE=aws`, so deployed Lambda remains the production default.

### 3. `localstack` → local SAM + LocalStack via Finch

Use this when you want to emulate AWS locally.

- Spring profile: `localstack`
- Runtime: `sam local invoke`
- Credentials: static test credentials
- Endpoint: `http://host.docker.internal:4566`
- DynamoDB behavior: table auto-creation enabled

`src/main/resources/application-localstack.yaml` configures this mode.

## Configuration summary

### Shared settings

`src/main/resources/application.yaml` contains the shared settings used by all modes:

- application name
- Spring Cloud Function definition
- default AWS region
- DynamoDB table name
- BPM URL and auth environment binding

### Local real-AWS settings

`src/main/resources/application-local.yaml`

- switches Spring Boot to servlet mode
- exposes port `8080`
- clears the default function definition so the bean name becomes the single HTTP path
- reads the AWS CLI profile from `AWS_PROFILE`

### LocalStack settings

`src/main/resources/application-localstack.yaml`

- sets the LocalStack endpoint
- uses static test credentials
- enables local table creation

## SQS payload shape

The Lambda-oriented path expects a standard SQS event wrapper with a `Records` array. Each record contains a `body` string, and that `body` string contains the email instruction JSON.

In other words, there are two nested JSON layers:

- outer JSON: the AWS SQS event
- inner JSON: the actual email payload used by the application

### Example outer event

```json
{
  "Records": [
    {
      "messageId": "12345",
      "body": "{\"from\":\"test@example.com\",\"sender\":\"tester\",\"subject\":\"hello\",\"receivedDateTime\":\"2026-10-01T12:00:00Z\",\"emailAttachments\":[]}",
      "eventSource": "aws:sqs",
      "awsRegion": "af-south-1"
    }
  ]
}
```

### Example inner body payload

```json
{
  "from": "test@example.com",
  "sender": "tester",
  "subject": "hello",
  "receivedDateTime": "2026-10-01T12:00:00Z",
  "emailAttachments": []
}
```

### Local HTTP testing shape

The local HTTP endpoint uses the same outer SQS/Lambda event payload shape as AWS Lambda. For local dev testing, post the full event to:

- `POST /flattenPDF`

## Verified local real-AWS flow

This flow was verified with the `muzi-mac-profile` AWS CLI profile.

### Start the app

```bash
cd /Users/muzi.phage/sbsa-workspace/aws/cibwa-ocr-digital-pdf-flatten-xfa
AWS_PROFILE=muzi-mac-profile ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Send a test request

```bash
curl -i -X POST http://localhost:8080/flattenPDF \
  -H 'Content-Type: application/json' \
  --data @events/sqs-dynamodb-insert-event.json
```

Expected result:

- HTTP `202 Accepted`
- a new item written to the `EmailInfo` DynamoDB table in AWS

## Local SAM + LocalStack flow

Build the application for SAM:

```bash
sam build --template template.yaml
```

Invoke the Lambda with the LocalStack environment file:

```bash
sam local invoke FlattenXfaFunction \
  --env-vars events/env-vars.localstack.json \
  --event events/sqs-dynamodb-insert-event.json
```

Smaller sample event:

```bash
sam local invoke FlattenXfaFunction \
  --env-vars events/env-vars.localstack.json \
  --event events/sqs-event.json
```

### SAM env-var files

- `events/env-vars.localstack.json` → local SAM + LocalStack
- `events/env-vars.aws.json` → AWS-style environment file without the LocalStack endpoint
- `events/env-vars.json` → legacy compatibility file

## Deployed AWS Lambda flow

When deployed through SAM, `template.yaml` supplies:

- `MAIN_CLASS=za.co.ocr.DigitalPdfFlattenXfaApplication`
- `SPRING_CLOUD_FUNCTION_DEFINITION=flattenPDF`
- `SPRING_PROFILES_ACTIVE=aws`

The Lambda is triggered by SQS and uses the Lambda execution role to access DynamoDB.

## Troubleshooting

### Port `8080` is already in use

If the local servlet app does not start, check whether another Java process is already using port `8080`.

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
pkill -f 'spring-boot:run -Dspring-boot.run.profiles=local'
```

Then start the app again.

### AWS profile works in CLI but not in the app

The local real-AWS mode uses an AWS CLI profile, and SSO-backed profiles require the AWS SDK SSO modules on the application classpath. That support is already included in `pom.xml`.

Useful checks:

```bash
aws sts get-caller-identity --profile muzi-mac-profile
aws dynamodb describe-table --profile muzi-mac-profile --table-name EmailInfo --region af-south-1
```

If the AWS CLI works but the app fails, make sure you started the application with the profile in the same shell session:

```bash
AWS_PROFILE=muzi-mac-profile ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Which HTTP path should I call locally?

Use the functional bean name as the path:

```bash
curl -i -X POST http://localhost:8080/flattenPDF \
  -H 'Content-Type: application/json' \
  --data @events/sqs-dynamodb-insert-event.json
```

The local profile clears the default function definition specifically so the application exposes one explicit function path for local testing.

### LocalStack / Finch cannot reach the host

The LocalStack profile uses:

- `http://host.docker.internal:4566`

That is intentional. When SAM runs inside Finch, `localhost` inside the container is not the same as `localhost` on the Mac host. `host.docker.internal` is the container-safe way to reach the LocalStack service running on the host.

### `sam local invoke` fails with host port allocation issues

In this workspace, `sam local invoke` previously hit a Finch runtime issue similar to:

- `No free ports on the host machine from 5000 to 9000`

When that happens, it is a local container-runtime problem rather than an application-code problem. In that case, prefer one of these alternatives:

- use the `local` profile with Tomcat for local development against real AWS
- use LocalStack only when the Finch runtime is healthy
- deploy to AWS for end-to-end Lambda verification

### `sam validate` needs a region

If `sam validate` complains about region locally, pass one explicitly:

```bash
sam validate -t template.yaml --region af-south-1
```

