#!/usr/bin/env zsh

usage() {
  print "Usage: env.sh [[-a] [-d] [-p] [-s] [-w]] <env name>
  Simulate, apply changes to or destory an AWS environment.
  Environments are named using the current git branch name.
  -a    apply changes to an environment. If the environment does not exist, it will be created.
  -p    plan/simulate changes
  -d    destroy an environment
  -s    show an (almost) human readable view of the given environment
  -w    watch logs from the given environment
  "
  exit 1
}

apply_env() {
  terraform init -backend-config="path=.$env_name_short/terraform.tfstate" && terraform apply -var-file="$HOME/.secrets.tfvars"
}

destroy_env() {
  terraform init -backend-config="path=.$env_name_short/terraform.tfstate" && terraform destroy -var-file="$HOME/.secrets.tfvars"
}

plan_env() {
  terraform init -backend-config="path=.$env_name_short/terraform.tfstate" && terraform plan -var-file="$HOME/.secrets.tfvars"
}

show_env() {
    terraform init -backend-config="path=.$env_name_short/terraform.tfstate" && terraform show
}

watch_logs() {
  awslogs get "geud-log-group-$env_name_short" --watch --timestamp
}

# TODO substitute the values you require below.
write_vars() {
cat << EOF > env.auto.tfvars
geud_connector_psql_host     = "credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432"
geud_connector_psql_database = "geud_connector_dev"
geud_connector_psql_username = "geud_connector_dev"
connector_docker_image       = "895947072537.dkr.ecr.us-east-2.amazonaws.com/atala:d74d3e30cb92cfb6f8fc09d2a2feada040b92f0f"
env_name_short               = "$env_name_short"
EOF
}

action="plan"
while getopts ':adpsw' arg; do
  case $arg in
    (a) action="apply";;
    (d) action="destroy";;
    (p) action="plan";;
    (s) action="show";;
    (w) action="watch";;
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
esac