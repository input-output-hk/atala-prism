#!/usr/bin/env bash
set -euo pipefail

usage () {
  echo "Usage: env.sh [[-a] [-A] [-d] [-D] [-p] [-s] [-w] [-g]] <env name>
  Simulate, create or destory an AWS environment.

  Environments are named by extracting the 'ata-xxxx' prefix from the currently
  git branch or from the <env name> parameter passed as the last argument.

  The secrets required for the build are currently:
  - postgres_password (the database password for the postgres user)
  - bitcoind_password (the rpc password for bitcoind)
  Their values should either be stored in ~/.secrets.tfvars or set as
  environment variables with prefix TF_VAR_ (e.g. TF_VAR_postgres_password).

  Available flags:
  -a    apply changes to an environment. If the environment does not exist, it will be created.
  -A    apply changes with auto-approve set.
  -p    plan/simulate changes
  -d    destroy an environment
  -D    destory an environment with auto-approve set.
  -s    show an (almost) human readable view of the given environment
  -w    watch logs from the given environment
  -g    create a 'graph.svg' file showing the dependencies of all resources.
  -t    'taint'. Forces a reploy on the next apply.
  "
  exit 1
}

apply_env () {
  write_vars
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform apply ${1-} -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

destroy_env () {
  write_vars
  drop_schemas
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform destroy ${1-} -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

plan_env () {
  write_vars
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform plan -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
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
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate" && terraform taint "aws_ecs_task_definition.cvp-task-definition" && terraform taint "aws_ecs_task_definition.admin-task-definition"
}

# Terraform's postgres provider will not drop a schema that we have created tables in (i.e. does not do a cascading drop)
# See this issue https://github.com/terraform-providers/terraform-provider-postgresql/issues/101.
drop_schemas () {

  # We remove the schemas from the terraform state file.
  terraform init -backend-config="key=infra/services/$env_name_short/terraform.tfstate"
  terraform state rm "postgresql_schema.connector-schema"
  terraform state rm "postgresql_schema.node-schema"

  # Then do a cascading drop here.
  # (PGPASSWORD must be set in the environment)
  psql -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -U postgres -d postgres -c "DROP SCHEMA \"node-${env_name_short}\" CASCADE; DROP SCHEMA \"connector-${env_name_short}\" CASCADE;"
}

write_vars () {

  set_vars

mkdir -p ".terraform"

cat << EOF > ".terraform/$env_name_short.tfvars"
bitcoind_username       = "bitcoin"

connector_docker_image  = "$connector_docker_image"
node_docker_image       = "$node_docker_image"
web_docker_image        = "$web_docker_image"
admin_docker_image      = "$admin_docker_image"

env_name_short          = "$env_name_short"
EOF

}

get_tag() {
  component=$1
  version=$2
  tag=$(aws --output json ecr describe-images --filter tagStatus=TAGGED --repository-name "$component" | jq "[[.imageDetails[]] | sort_by(-.imagePushedAt)[] | .imageTags[]] | map(select(test(\"$version\")))[]" | head -1)
  echo -n "$tag"
}

set_vars () {
  version=$env_name_short

  echo -n "Querying ECR for environment images..."
  connector_tag=$(get_tag "connector" "$env_name_short")
  if [ -z "$connector_tag" ]; then
    connector_tag=$(get_tag "connector" "develop")
  fi
  if [ -z "$connector_tag" ]; then
    echo "No available connector image. Exiting."
    exit 1
  fi
  node_tag=$(get_tag "node" "$env_name_short")
  if [ -z "$node_tag" ]; then
    node_tag=$(get_tag "node" "develop")
  fi
  if [ -z "$node_tag" ]; then
    echo "No available node image. Exiting."
    exit 1
  fi
  web_tag=$(get_tag "web" "$env_name_short")
  if [ -z "$web_tag" ]; then
    web_tag=$(get_tag "web" "develop")
  fi
  if [ -z "$web_tag" ]; then
    echo "No available web image. Exiting."
    exit 1
  fi
  admin_tag=$(get_tag "admin" "$env_name_short")
  if [ -z "$admin_tag" ]; then
    admin_tag=$(get_tag "admin" "develop")
  fi
  if [ -z "$admin_tag" ]; then
    echo "No available admin image. Exiting."
    exit 1
  fi
  echo "done."
  # Remove leading/trailing quote marks from string values
  connector_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$connector_tag")
  node_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$node_tag")
  web_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$web_tag")
  admin_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$admin_tag")

  connector_docker_image="$ecr_url/connector:$connector_tag"
  node_docker_image="$ecr_url/node:$node_tag"
  web_docker_image="$ecr_url/web:$web_tag"
  admin_docker_image="$ecr_url/admin:$admin_tag"

  if [ -f "$HOME/.secrets.tfvars" ]; then
    secrets="-var-file=$HOME/.secrets.tfvars"
    echo "Using secrets file: $HOME/.secrets.tfvars"
  fi

  echo "Using connector image: $connector_docker_image"
  echo "Using node image: $node_docker_image"
  echo "Using web image: $web_docker_image"
  echo "Using admin image: $admin_docker_image"
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

env_name_default=$(git rev-parse --abbrev-ref HEAD | sed -E 's/(^ATA\-[0-9]+).*/\1/' | tr '[:upper:]' '[:lower:]')
env_name_short=${1-$env_name_default}
ecr_url="895947072537.dkr.ecr.us-east-2.amazonaws.com"

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
