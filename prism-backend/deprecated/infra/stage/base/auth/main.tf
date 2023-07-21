
variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

provider "aws" {
  region = "us-east-1"
}

// NB "us-east-1"
// This is important since all Lambda@Edge functions to be used with CloudFront must be in this region.
provider "aws" {
  region  = "us-east-1"
  profile = var.aws_profile
  alias   = "us-east-1"
}

data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com", "edgelambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lambda_edge_role" {
  name = "lambda-edge-role"

  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

resource "aws_iam_role_policy" "lambda_access_policy" {
  name   = "lambda_access_policy"
  role   = aws_iam_role.lambda_edge_role.id
  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogStream",
        "logs:CreateLogGroup",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
POLICY
}

data "archive_file" "basic_auth" {
  type        = "zip"
  source_dir  = "lambda/basic_auth"
  output_path = "lambda/dst/basic_auth.zip"
}

resource "aws_lambda_function" "basic_auth" {
  provider         = aws.us-east-1
  filename         = data.archive_file.basic_auth.output_path
  function_name    = "basic_auth"
  role             = aws_iam_role.lambda_edge_role.arn
  handler          = "index.handler"
  source_code_hash = data.archive_file.basic_auth.output_base64sha256
  runtime          = "nodejs12.x"
  description      = "Protect CloudFront distributions with Basic Authentication"
  publish          = true
}

resource "aws_cloudwatch_log_group" "basic_auth_log_group" {
  name              = "/aws/lambda/basic_auth"
  retention_in_days = 5
}