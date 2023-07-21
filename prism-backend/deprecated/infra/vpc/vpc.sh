#!/usr/bin/env zsh
set -e

usage() {
  print "Usage: vpc.sh [[-a] [-d] [-p] [-s]] <env type>
  Setup the base network for supporting a given type of environment, such as 'test' or 'prod'.
  -a    apply changes.
  -p    plan/simulate changes
  -d    destroy
  -s    show an (almost) human readable view of the given config
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="path=.$env_type/terraform.tfstate" && terraform apply -var "env_type=$env_type"
}

destroy_env() {
  terraform init -backend-config="path=.$env_type/terraform.tfstate" && terraform destroy -var "env_type=$env_type"
}

plan_env() {
  terraform init -backend-config="path=.$env_type/terraform.tfstate" && terraform plan -var "env_type=$env_type"
}

show_env() {
    terraform init -backend-config="path=.$env_type/terraform.tfstate" && terraform show
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

env_type=$1
if [[ -z "${env_type// }" ]]; then
  env_type='test'
fi

print "Using env type '$env_type'."
print "Performing action '$action'."

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
esac