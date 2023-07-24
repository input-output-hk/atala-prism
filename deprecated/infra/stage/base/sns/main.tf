
variable "aws_profile" {
  description = "The AWS CLI profile to use."
  default     = "default"
}

// NB: cloudwatch alarms and associated SNS topic MUST be in us-east-1 (NOT us-east-2)
// (https://docs.aws.amazon.com/Route53/latest/DeveloperGuide/monitoring-health-checks.html)
provider "aws" {
  region  = "us-east-1"
  profile = var.aws_profile
}

variable "sns_topic_name" {
  description = "The topic name for monitoring alerts"
  default     = "atala-prism-service-alerts"
}

variable "slack_channel_name" {
  description = "The slack channel where monitoring should be sent."
  default     = "atala-prism-service-alerts"
}

variable "slack_webhook_url" {
  description = "Webhook URL for the slack monitoring alerts channel. This should be stored in your .secrets.tfvars file."
}

variable "slack_display_user" {
  description = "The username to use for reporting monitoring events to slack"
  default     = "reporter"
}

module "notify_slack" {
  source = "terraform-aws-modules/notify-slack/aws"

  sns_topic_name    = var.sns_topic_name
  slack_channel     = var.slack_channel_name
  slack_username    = var.slack_display_user
  slack_webhook_url = var.slack_webhook_url

  lambda_description = "Lambda function which sends notifications to Slack"
  log_events         = true

  tags = {
    Name = "atala-prism-service-alerts-slack-subscriber"
  }
}
