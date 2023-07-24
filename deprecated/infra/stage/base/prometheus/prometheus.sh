#!/usr/bin/env zsh
set -e

usage() {
  print "Usage: prometheus.sh [[-a] [-d] [-p] [-s]] <vpc name>
  Setup Prometheus monitoring for supporting environments.
  <vpc name> should be the same name as used for vpc creation.
  -a    apply changes.
  -p    plan/simulate changes
  -d    destroy
  -s    show an (almost) human readable view of the given config
  -g    create a 'graph.svg' file showing the dependencies of all resources.
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

graph_env () {
  terraform init -backend-config="key=$state_key" && terraform graph -draw-cycles | dot -Tsvg > graph.svg
}

action="plan"
while getopts ':adpsg' arg; do
  case $arg in
    (a) action="apply";;
    (d) action="destroy";;
    (p) action="plan";;
    (s) action="show";;
    (g) action="graph";;
    (\*) usage
         exit 1;;
    (\?) usage
         exit 1;;
  esac
done
shift $((OPTIND -1))

name=$1
if [[ -z "${name// }" ]]; then
  name='prism-test'
fi

state_key="infra/stage/prometheus/$name/terraform.tfstate"

print "Using name '$name'."
print "Performing action '$action'."

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
  (graph) graph_env;;
esac
