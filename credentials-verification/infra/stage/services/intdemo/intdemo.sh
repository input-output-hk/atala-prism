#!/usr/bin/env bash
set -euo pipefail

usage () {
  echo "Usage: intdemo.sh [[-a] [-A] [-d] [-D] [-p] [-s] [-w] [-g]] <env name>
  Simulate, create or destory an AWS environment for intdemo.

  Environments are named by extracting the 'ata-xxxx' prefix from the currently
  git branch or from the <env name> parameter passed as the last argument.

  The secrets required for the build are currently:
  - postgres_password (the database password for the postgres user)
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
  terraform init -backend-config="key=$state_key" && terraform apply ${1-} -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

destroy_env () {
  write_vars
  drop_schemas
  terraform init -backend-config="key=$state_key" && terraform destroy ${1-} -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

plan_env () {
  write_vars
  terraform init -backend-config="key=$state_key" && terraform plan -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

show_env () {
  terraform init -backend-config="key=$state_key" && terraform show
}

watch_logs () {
  awslogs get "intdemo-log-group-$env_name_short" --watch --timestamp
}

graph_env () {
  terraform init -backend-config="key=$state_key" && terraform graph -draw-cycles | dot -Tsvg > graph.svg
}

taint_env () {
    task_definition_path="module.intdemo_service.aws_ecs_task_definition.intdemo_task_definition"
    terraform init -backend-config="key=$state_key"
    if terraform state show $task_definition_path > /dev/null; then
        echo "Tainting the environment"
        terraform taint $task_definition_path
    else
        echo "No environment to taint"
    fi
}

# Terraform's postgres provider will not drop a schema that we have created tables in (i.e. does not do a cascading drop)
# See this issue https://github.com/terraform-providers/terraform-provider-postgresql/issues/101.
drop_schemas () {

  # We remove the schemas from the terraform state file.
  terraform init -backend-config="key=$state_key"
  terraform state rm "postgresql_schema.connector-schema"

  # Then do a cascading drop here.
  # (PGPASSWORD must be set in the environment)
  psql -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -U postgres -d postgres -c "DROP SCHEMA \"intdemo-connector-${env_name_short}\" CASCADE;"
}

write_vars () {

  set_vars

mkdir -p ".terraform"

cat << EOF > ".terraform/$env_name_short.tfvars"
connector_docker_image  = "$connector_docker_image"
landing_docker_image    = "$landing_docker_image"

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
  landing_tag=$(get_tag "landing" "$env_name_short")
  if [ -z "$landing_tag" ]; then
    landing_tag=$(get_tag "landing" "develop")
  fi
  if [ -z "$landing_tag" ]; then
    echo "No available web image. Exiting."
    exit 1
  fi
  echo "done."
  # Remove leading/trailing quote marks from string values
  connector_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$connector_tag")
  landing_tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$landing_tag")

  connector_docker_image="$ecr_url/connector:$connector_tag"
  landing_docker_image="$ecr_url/landing:$landing_tag"

  if [ -f "$HOME/.secrets.tfvars" ]; then
    secrets="-var-file=$HOME/.secrets.tfvars"
    echo "Using secrets file: $HOME/.secrets.tfvars"
  fi

  echo "Using connector image: $connector_docker_image"
  echo "Using landing image: $landing_docker_image"
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

state_key="infra/stage/services/intdemo/$env_name_short/terraform.tfstate"

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
