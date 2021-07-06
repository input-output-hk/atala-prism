#!/usr/bin/env bash
set -euo pipefail

usage() {
  print "Usage: sns.sh [[-a] [-d] [-p] [-s]] sns-topic-name
  Setup sns topic for monitoring alerts. Default topic name is atala-prism-service-alerts.
  -a    apply changes
  -p    plan/simulate changes
  -d    destroy
  -s    show an (almost) human readable view of the given config
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="key=$state_key" && terraform apply -var "sns_topic_name=$sns_topic_name" ${secrets-}
}

destroy_env() {
  terraform init -backend-config="key=$state_key" && terraform destroy -var "sns_topic_name=$sns_topic_name" ${secrets-}
}

plan_env() {
  terraform init -backend-config="key=$state_key" && terraform plan -var "sns_topic_name=$sns_topic_name" ${secrets-}
}

show_env() {
  terraform init -backend-config="key=$state_key" && terraform show
}

action="plan"
while getopts ':adps' arg; do
  case $arg in
    (a) action="apply";;
    (d) action="destroy";;
    (p) action="plan";;
    (s) action="show";;
    (\*) usage
         exit 1;;
    (\?) usage
         exit 1;;
  esac
done
shift $((OPTIND -1))

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null

sns_topic_name=${1:-atala-prism-service-alerts}

state_key="infra/base/sns/$sns_topic_name/terraform.tfstate"

source ../../../functions.sh
secrets=$(read_secrets)
echo "Using sns topic name $sns_topic_name."
echo "Performing action '$action'."
echo "Using secrets file $secrets."

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
esac
