#!/usr/bin/env bash
set -e

usage () {
  echo "Usage: env.sh [[-a] [-A] [-d] [-D] [-p] [-s] [-w] [-g]] <env name>
  Simulate, create or destory an AWS environment.

  Environments are named by extracting the 'ata-xxxx' prefix from the currently
  git branch or from the <env name> parameter passed as the last argument.

  The secrets required for the build are currently:
  - connector_psql_password
  - node_psql_password
  - bitcoind_password
  Their values should either be stored in ~/.secrets.tfvars or set as
  environment variables with prefix TF_VAR_ (e.g. TF_VAR_connector_plsql_password).

  Available flags:
  -a    apply changes to an environment. If the environment does not exist, it will be created.
  -A    apply changes with auto-approve set.
  -p    plan/simulate changes
  -d    destroy an environment
  -D    destory an environment with auto-approve set.
  -s    show an (almost) human readable view of the given environment
  -w    watch logs from the given environment
  -g    create a 'graph.svg' file showing the dependencies of all resources.
  -t    'taint'. Forces recreation of the ECS service (e.g. after updating a task definition).
         Note, this will cause some downtime.
  "
  exit 1
}

apply_env () {
  write_vars
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform apply $1 -var-file=".terraform/$env_name_short.tfvars" $secrets
}

destroy_env () {
  write_vars
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform destroy $1 -var-file=".terraform/$env_name_short.tfvars" $secrets
}

plan_env () {
  write_vars
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform plan -var-file=".terraform/$env_name_short.tfvars" $secrets
}

show_env () {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform show
}

watch_logs () {
  awslogs get "cvp-log-group-$env_name_short" --watch --timestamp
}

graph_env () {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform graph -draw-cycles | dot -Tsvg > graph.svg
}

taint_env () {
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform taint "aws_ecs_service.cvp-service"
}

write_vars () {

  set_vars

mkdir -p ".terraform"

cat << EOF > ".terraform/$env_name_short.tfvars"
bitcoind_username       = "bitcoin"

connector_docker_image  = "$connector_docker_image"
node_docker_image       = "$node_docker_image"
web_docker_image        = "$web_docker_image"

env_name_short          = "$env_name_short"
EOF

}

set_vars () {
  version=$env_name_short

  echo -n "Querying ECR for environment images..."
  connector_branch_count=$(aws --output json ecr list-images --repository-name connector | jq "[.imageIds[] | select(.imageTag == \"$version\")] | length")
  node_branch_count=$(aws --output json ecr list-images --repository-name connector | jq "[.imageIds[] | select(.imageTag == \"$version\")] | length")
  web_branch_count=$(aws --output json ecr list-images --repository-name connector | jq "[.imageIds[] | select(.imageTag == \"$version\")] | length")
  echo "done."
  connector_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/connector:develop"
  [ "$connector_branch_count" -ne 0 ] && connector_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/connector:$version"

  node_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/node:develop"
  [ "$node_branch_count" -ne 0 ] && node_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/node:$version"

  web_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/web:develop"
  [ "$web_branch_count" -ne 0 ] && web_docker_image="895947072537.dkr.ecr.us-east-2.amazonaws.com/web:$version"

  if [ -f "$HOME/.secrets.tfvars" ]; then
    secrets="-var-file=$HOME/.secrets.tfvars"
    echo "Using secrets file: $HOME/.secrets.tfvars"
  fi

  echo "Using connector image: $connector_docker_image"
  echo "Using node image: $node_docker_image"
  echo "Using web image: $web_docker_image"
}

action="plan"
while getopts ':aAdDpswgt' arg; do
  case $arg in
    (a) action="apply";;
    (A) action="auto-apply";;
    (d) action="destroy";;
    (D) action="auto-destroy";;
    (p) action="plan";;
    (s) action="show";;
    (w) action="watch";;
    (g) action="graph";;
    (t) action="taint";;
    (\*) usage
         exit 1;;
    (\?) usage
         exit 1;;
  esac
done
shift $((OPTIND -1))

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null

env_name_short=$1
if [[ -z "${env_name_short// }" ]]; then
  env_name_short=$(git rev-parse --abbrev-ref HEAD | sed -E 's/(^ATA\-[0-9]+).*/\1/' | tr '[:upper:]' '[:lower:]')
fi

echo "Using env name '$env_name_short'."
echo "Performing action '$action'."

case $action in
  (apply) apply_env;;
  (auto-apply) apply_env -auto-approve;;
  (destroy) destroy_env;;
  (auto-destroy) destroy_env -auto-approve;;
  (plan) plan_env;;
  (show) show_env;;
  (watch) watch_logs;;
  (graph) graph_env;;
  (taint) taint_env;;
esac

popd
