#!/usr/bin/env bash
set -euo pipefail

usage() {
  print "Usage: auth.sh [[-a] [-d] [-p] [-s]]
  Setup basic auth using lambda@edge for protecting website used with CloudFront.
  -a    apply changes
  -p    plan/simulate changes
  -d    destroy
  -s    show an (almost) human readable view of the given config
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="key=$state_key" && terraform apply
}

destroy_env() {
  terraform init -backend-config="key=$state_key" && terraform destroy
}

plan_env() {
  terraform init -backend-config="key=$state_key" && terraform plan
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

state_key="infra/stage/auth/terraform.tfstate"

echo "Using lambda@edge basic auth"
echo "Performing action '$action'."

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
esac
