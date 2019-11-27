#!/usr/bin/env zsh
set -e

usage() {
  print "Usage: env.sh [[-a] [-d] [-p] [-s] [-w] [-g]] <env name>
  Simulate, apply changes to or destory an AWS environment.
  Environments are named using the current git branch name.
  -a    apply changes to an environment. If the environment does not exist, it will be created.
  -p    plan/simulate changes
  -d    destroy an environment
  -s    show an (almost) human readable view of the given environment
  -w    watch logs from the given environment
  -g    create a 'graph.svg' file showing the dependencies of all resources.
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform apply -var-file="$HOME/.secrets.tfvars"
}

destroy_env() {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform destroy -var-file="$HOME/.secrets.tfvars"
}

plan_env() {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform plan -var-file="$HOME/.secrets.tfvars"
}

show_env() {
    terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform show
}

watch_logs() {
  awslogs get "cvp-log-group-$env_name_short" --watch --timestamp
}

graph_env() {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform graph -draw-cycles | dot -Tsvg > graph.svg
}

# TODO substitute the values you require below.
write_vars() {
cat << EOF > env.auto.tfvars
connector_psql_host     = "credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432"
connector_psql_database = "geud_connector_dev"
connector_psql_username = "geud_connector_dev"
connector_docker_image  = "895947072537.dkr.ecr.us-east-2.amazonaws.com/atala:connector-541af52a1f586c179f180b4bfb372e56ba6797c7"

node_psql_host          = "credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432"
node_psql_database      = "geud_node_dev"
node_psql_username      = "geud_node_dev"
node_docker_image       = "895947072537.dkr.ecr.us-east-2.amazonaws.com/atala:node-cf79abe55475803fda92e1379f8a39b97f6fbdfd"

bitcoind_username       = "bitcoin"

env_name_short          = "$env_name_short"
EOF
}

action="plan"
while getopts ':adpswg' arg; do
  case $arg in
    (a) action="apply";;
    (d) action="destroy";;
    (p) action="plan";;
    (s) action="show";;
    (w) action="watch";;
    (g) action="graph";;
    (\*) usage
         exit 1;;
    (\?) usage
         exit 1;;
  esac
done
shift $((OPTIND -1))

env_name_short=$1
if [[ -z "${env_name_short// }" ]]; then
  usage
fi

print "Using env name '$env_name_short'."
print "Performing action '$action'."

write_vars

case $action in
  (apply) apply_env;;
  (destroy) destroy_env;;
  (plan) plan_env;;
  (show) show_env;;
  (watch) watch_logs;;
  (graph) graph_env;;
esac