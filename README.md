Utilizing Spring Cloud Function to streamline and flatten digital PDF documents. Achieved by using iText licensed library to remove xfa forms and flatten the PDF. The application is built using Spring Cloud Function and can be deployed as a serverless function on platforms like AWS Lambda.

## Profile activation

This project keeps AWS deployment defaults separate from local SAM testing overrides.

### Default application behavior

- `application.yaml` does **not** force a default Spring profile
- If no profile is explicitly set, the app uses the shared base configuration only
- Shared settings such as the DynamoDB table name live in `application.yaml`

### Deployed AWS Lambda behavior

`template.yaml` sets:

- `SPRING_PROFILES_ACTIVE=aws`

That means deployed Lambdas use the `aws` profile by default, which relies on the normal AWS credential chain / Lambda execution role and does **not** use a LocalStack endpoint override.

### Local SAM behavior

`events/env-vars.localstack.json` sets the local-only overrides for SAM:

- `SPRING_PROFILES_ACTIVE=localstack`
- `BPM_AUTH=...`

This keeps LocalStack-specific configuration out of the deploy template defaults.

If you see an error like `Connect to http://localhost:4566 ... Connection refused`, it usually means the `localstack` profile is active but LocalStack is not running. With the current setup, that should only happen when you explicitly activate `localstack` (for example via `events/env-vars.localstack.json` during local SAM runs).

## SAM env-var file conventions

- `events/env-vars.localstack.json` → local SAM + LocalStack
- `events/env-vars.aws.json` → AWS-style profile without LocalStack endpoint override
- `events/env-vars.json` → legacy local file kept for backward compatibility

## Available profiles

### `aws`

- Used for deployed AWS Lambda
- Uses default AWS credentials resolution
- Expects the DynamoDB table to already exist

### `localstack`

- Used for local SAM + LocalStack runs
- Uses the LocalStack endpoint defined in `application-localstack.yaml`
- Enables local DynamoDB table auto-creation

### `local`

- Optional local profile for using an AWS CLI named profile
- Reads `AWS_PROFILE` from the environment

## Run locally with SAM

Build the function:

```bash
sam build --template template.yaml
```

Start the local API:

```bash
sam local start-api --template .aws-sam/build/template.yaml --env-vars events/env-vars.localstack.json --port 3001
```

Start the local API without LocalStack endpoint overrides:

```bash
sam local start-api --template .aws-sam/build/template.yaml --env-vars events/env-vars.aws.json --port 3001
```

Example request:

```bash
curl -i -X POST http://127.0.0.1:3001/flatten \
  -H "Content-Type: application/json" \
  -d '{"records":[{"messageId":"1","body":"{\"from\":\"a@x.com\",\"sender\":\"Admin\",\"subject\":\"Test\",\"receivedDateTime\":\"2026-10-01T00:00:00Z\",\"emailAttachments\":[]}"}]}'
```

## Notes

- If `sam validate` complains about a missing region locally, pass one explicitly, for example:

```bash
sam validate -t template.yaml --region af-south-1
```

- `events/env-vars.localstack.json` and `events/env-vars.aws.json` are the preferred explicit options for local testing.
- `events/env-vars.json` is retained as a compatibility alias for the older local flow.
