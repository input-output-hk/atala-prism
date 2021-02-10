#!/usr/bin/env bash
set -euo pipefail

usage () {
  echo "Usage: prism.sh [[-a] [-A] [-d] [-D] [-p] [-s] [-w] [-g] [-c] [-m]] <env name>
  Simulate, create or destory an AWS environment for prism.

  Environments are named by extracting the 'ata-xxxx' prefix from the currently
  git branch or from the <env name> parameter passed as the last argument.

  The secrets required for the build are currently:
  - postgres_password (the database password for the postgres user)
  Their values should either be stored in ~/.secrets.tfvars or set as
  environment variables with prefix TF_VAR_ (e.g. TF_VAR_postgres_password).

  Available flags:
  -a    apply changes to an environment. If the environment does not exist, it will be created.
        Note also, that the environment variables file, .tarraform/<env name>.tfvars, which specifies
        the component docker images, will not be overwritten when using this flag. Thus, use -a if
        you wish to make manual edits to the environment config.
        If you are doing a production deployment, and want to double check the config before pressing 'go',
        this is probably the flag for you. You should first this script with '-p' set, which will write the
        vars file to your disk, check the values, optionally make manual edits to the component versions,
        then run this script with -a.
  -A    apply changes with auto-approve set. Best suited to automatic deployment of the latest docker
        image versions pertaining to your branch.
        If you developing new features and want to push your latest changes to an env, this is probably the flag
        you want.
  -p    plan/simulate changes. As well as running the terraform plan, this step queries ECR for the docker images
        relevant to your branch and writes their tags to an environment config in .terraform/<env name>.tfvars.
        You can thus use plan to generate and verify an environment config. If you wish to make manual edits to that
        config, you run with -p, make your edits, then apply with -a.
  -d    destroy an environment
  -D    destroy an environment with auto-approve set.
  -s    show an (almost) human readable view of the given environment
  -w    watch logs from the given environment
  -g    create a 'graph.svg' file showing the dependencies of all resources.
  -t    'taint'. Forces a reploy on the next apply.
  -c    'clean', Remove .terraform/<env name>.tfvars file if it exists.
  -m    'monitor'. Set to enable monitoring alerts from this environment to the atala-prism-service-alerts channel.
  "
  exit 1
}

apply_env () {
  write_vars "${1-}"
  terraform init -backend-config="key=$state_key" && terraform apply ${1-} -var-file=".terraform/$env_name_short.tfvars" ${secrets-}
}

destroy_env () {
  write_vars "${1-}"
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
  awslogs get "$env_name_short-prism-log-group" --watch --timestamp
}

graph_env () {
  terraform init -backend-config="key=$state_key" && terraform graph -draw-cycles | dot -Tsvg > graph.svg
}

taint_env () {
    terraform init -backend-config="key=$state_key"

    # read lines with active modules in state into a bash array
    # https://stackoverflow.com/questions/11426529/reading-output-of-a-command-into-an-array-in-bash
    mapfile -t aws_services_paths < <(terraform show | grep -Po '(?<=# )(.*aws_ecs_service.[^.:]+)')

    if [ ${#aws_services_paths[@]} -eq 0 ]; then
        echo "No service to taint"
    fi

    for path in ${aws_services_paths[@]}; do
        echo "Tainting $path"
        terraform taint $path
    done
}

# Terraform's postgres provider will not drop a schema that we have created tables in (i.e. does not do a cascading drop)
# See this issue https://github.com/terraform-providers/terraform-provider-postgresql/issues/101.
drop_schemas () {

  # We remove the schemas from the terraform state file.
  terraform init -backend-config="key=$state_key"
  terraform state rm "postgresql_schema.connector-schema"
  terraform state rm "postgresql_schema.management-console-schema"

  # Then do a cascading drop here.
  # (PGPASSWORD must be set in the environment)
  psql -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -U postgres -d postgres -c "DROP SCHEMA IF EXISTS \"prism-connector-${env_name_short}\" CASCADE;"
  psql -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -U postgres -d postgres -c "DROP SCHEMA IF EXISTS \"prism-node-${env_name_short}\" CASCADE;"
  psql -h credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com -U postgres -d postgres -c "DROP SCHEMA IF EXISTS \"prism-management-console-${env_name_short}\" CASCADE;"
}

write_vars () {

  # This block enables the user to explicitly specify the docker image versions.
  # This will be more suitable for production deployments.
  if [ "${1-}" == "-auto-approve" ] || [ ! -f "$tf_vars_file" ]; then
    echo "Writing vars file $tf_vars_file"
    set_vars
    mkdir -p ".terraform"
    cat << EOF > ".terraform/$env_name_short.tfvars"
# Generated by prism.sh on `date`
# The name of the environment. This is used as a prefix to most
# of the AWS resources. Editing this value is strongly discouraged.
# Doing so may lead to bizarre results.
env_name_short                = "$env_name_short"

# Tags for the component docker images.
# The prism script populates these values by querying ECR
# for the latest image versions, under the assumption this is
# usually want you want. For rollback, or other scenarios where
# you want a specific component version, edit these values according to your needs.
connector_docker_image              = "$connector_docker_image"
node_docker_image                   = "$node_docker_image"
management_console_docker_image     = "$management_console_docker_image"
landing_docker_image                = "$landing_docker_image"
prism_sdk_website_docs_docker_image = "$prism_sdk_website_docs_docker_image"
prism_console_docker_image          = "$prism_console_docker_image"
prism_lb_envoy_docker_image         = "$prism_lb_envoy_docker_image"

intdemo_enabled = $intdemo_enabled
geud_enabled    = $geud_enabled

# Toggle alerts to slack. 1 for on, 0 for off.
monitoring_alerts_enabled          = "$monitor"
EOF
  else
    echo "Note: .terraform/$env_name_short.tfvars already exists, will not write variables"
  fi
}

set_vars () {
  connector_docker_image=$(get_docker_image "connector" "$env_name_short" "develop")
  node_docker_image=$(get_docker_image "node" "$env_name_short" "develop")
  management_console_docker_image=$(get_docker_image "management-console" "$env_name_short" "develop")
  landing_docker_image=$(get_docker_image "landing" "$env_name_short" "develop")
  prism_sdk_website_docs_docker_image=$(get_docker_image "prism-docs-website" "$env_name_short" "develop")
  prism_console_docker_image=$(get_docker_image "web" "$env_name_short" "develop")
  prism_lb_envoy_docker_image=$(get_docker_image "prism-lb-envoy" "$env_name_short" "develop")

  if [ -f "$HOME/.secrets.tfvars" ]; then
    secrets="-var-file=$HOME/.secrets.tfvars"
    echo "Using secrets file: $HOME/.secrets.tfvars"
  fi

  echo "Using connector image: $connector_docker_image"
  echo "Using node image: $node_docker_image"
  echo "Using management console image: $management_console_docker_image"
  echo "Using landing image: $landing_docker_image"
  echo "Using prisk website docs image: $prism_sdk_website_docs_docker_image"
  echo "Using prism console image: $prism_console_docker_image"
  echo "Using envoy image: $prism_lb_envoy_docker_image"
}

clean_config() {
  rm -f "$tf_vars_file"
}

action="plan"
monitor="0"
while getopts ':aAdDpswgtcm' arg; do
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
    (c) action="clean-config";;
    (m) monitor="1";;
    (\*) usage
         exit 1;;
    (\?) usage
         exit 1;;
  esac
done
shift $((OPTIND -1))

dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
pushd "$dir" > /dev/null

source ../../../functions.sh

env_name_default=$(get_branch_prefix)
env_name_short=${1-$env_name_default}
tf_vars_file=".terraform/$env_name_short.tfvars"
state_key="infra/stage/services/prism/$env_name_short/terraform.tfstate"

intdemo_enabled=${INTDEMO_ENABLED:-"false"}
geud_enabled=${GEUD_ENABLED:-"false"}

if [ -z ${TF_VAR_cardano_confirmation_blocks:-} ] && [ -n "${NODE_CARDANO_CONFIRMATION_BLOCKS:-}" ]; then
  export TF_VAR_cardano_confirmation_blocks=$NODE_CARDANO_CONFIRMATION_BLOCKS
fi

if [ -z ${TF_VAR_cardano_wallet_id:-} ] && [ -n "${NODE_CARDANO_WALLET_ID:-}" ]; then
  export TF_VAR_cardano_wallet_id=$NODE_CARDANO_WALLET_ID
fi

if [ -z ${TF_VAR_cardano_wallet_passphrase:-} ] && [ -n "${NODE_CARDANO_WALLET_PASSPHRASE:-}" ]; then
  export TF_VAR_cardano_wallet_passphrase=$NODE_CARDANO_WALLET_PASSPHRASE
fi

if [ -z ${TF_VAR_cardano_payment_address:-} ] && [ -n "${NODE_CARDANO_PAYMENT_ADDRESS:-}" ]; then
  export TF_VAR_cardano_payment_address=$NODE_CARDANO_PAYMENT_ADDRESS
fi

echo "Using env name '$env_name_short'."
echo "Performing action '$action'."

case $intdemo_enabled in
  true  ) echo "Intdemo enabled";;
  false ) echo "Intdemo disabled, use INTDEMO_ENABLED=true to enable";;
  *     ) echo "Warning: unknown value INTDEMO_ENABLED=$intdemo_enabled";;
esac

case $geud_enabled in
  true  ) echo "GEUD enabled";;
  false ) echo "GEUD disabled, use GEUD_ENABLED=true to enable";;
  *     ) echo "Warning: unknown value GEUD_ENABLED=$geud_enabled";;
esac

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
  (clean-config) clean_config;;
esac

popd > /dev/null
