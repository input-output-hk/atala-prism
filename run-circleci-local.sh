#!/usr/bin/env bash

# Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in your environment
# (do not paste them into this script)
# Edit the job name below as neccessary.

# As of writing, the circleci command line tool does not support v2.1 syntax directly.
circleci config process .circleci/config.yml > .circleci/.process-v2.yml

circleci local execute \
  -c .circleci/.process-v2.yml \
  -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
  -e AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID}" \
  -e AWS_DEFAULT_REGION=us-east-2 \
  -e AWS_ECR_ACCOUNT_URL=895947072537.dkr.ecr.us-east-2.amazonaws.com \
  -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
  --job deploy-ecr-images
