#!/bin/bash
# Script to run SAM local invoke with AWS profile credentials
# This sources the AWS CLI profile and passes credentials to SAM in the container

PROFILE="muzi-mac-profile"
REGION="af-south-1"

# Verify the profile is configured
aws sts get-caller-identity --profile $PROFILE > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Error: Profile '$PROFILE' not found or credentials expired"
    echo "Try: aws sso login --profile $PROFILE"
    exit 1
fi

echo "Running SAM local invoke with AWS profile: $PROFILE"
echo ""

# Run SAM with the AWS_PROFILE environment variable
# SAM will need access to the credentials, so we'll use a workaround:
# Mount the AWS config directory and pass the profile name
AWS_PROFILE="$PROFILE" \
sam local invoke FlattenXfaFunction \
  --env-vars events/env-vars.local-aws.json \
  --event events/sqs-dynamodb-insert-event.json \
  --container-host-interface host.docker.internal \
  "$@"


