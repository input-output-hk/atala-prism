#!/usr/bin/env zsh
set -e

usage() {
  print "Usage: tls.sh [[-a] [-d] [-p] [-s]] <domain>
  Setup TLS certificate in ACM.
  <domain> is a domain name controlled by the account
  -a    apply changes
  -p    plan/simulate changes
  -d    destroy
  -s    show an (almost) human readable view of the given config
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="key=$state_key" && terraform apply -var "name=$name"
}

destroy_env() {
  terraform init -backend-config="key=$state_key" && terraform destroy -var "name=$name"
}

plan_env() {
  terraform init -backend-config="key=$state_key" && terraform plan -var "name=$name"
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

name=$1
if [[ -z "${name// }" ]]; then
  name='cef.iohkdev.io'
fi

state_key="infra/base/tls/$name/terraform.tfstate"

print "Using domain name '$name'."
print "Performing action '$action'."

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
esac
