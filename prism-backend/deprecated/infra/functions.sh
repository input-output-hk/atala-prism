
ecr_url="895947072537.dkr.ecr.us-east-2.amazonaws.com"


#
# These functions generate a new version string, for publishing to the ECR registry using docker push
#
next_docker_tag() {
  component=$1
  tag=$(next_tag)
  docker_tag="$ecr_url/$component:$tag"
  echo -n "$docker_tag"
}

next_tag() {
  branchPrefix=$(get_branch_prefix)
  revCount=$(git rev-list HEAD --count)
  shaShort=$(git rev-parse --short HEAD)
  tag="${branchPrefix}-${revCount}-${shaShort}"
  echo -n "$tag"
}

get_branch_prefix() {
  git rev-parse --abbrev-ref HEAD | sed -E 's/(^[aA][tT][aA]\-[0-9]+).*/\1/' | tr '[:upper:]' '[:lower:]'
}


#
# These functions query the ECR registry for the latest published versions, for deployment to AWS using terraform.
#
get_tag() {
  component=$1
  tag=$2
  tag=$(aws --output json ecr describe-images --filter tagStatus=TAGGED --repository-name "$component" | jq "[[.imageDetails[]] | sort_by(.imagePushedAt)[] | .imageTags[]] | map(select(test(\"$tag\")))[]" | tail -1)
  tag=$(sed -e 's/^"//' -e 's/"$//' <<< "$tag")
  echo -n "$tag"
}

get_tag_with_fallback() {
  component=$1
  tag=$2
  fallback=$3
  tag=$(get_tag "$component" "$tag")

  if [ -z "$tag" ]; then
    tag=$(get_tag "$component" "$fallback")
  fi
  if [ -z "$tag" ]; then
    echo "No available image for component $1 with tag $2. Exiting."
    exit 1
  fi

  echo -n "$tag"
}

get_docker_image() {
  component=$1
  tag=$2
  fallback=$3
  tag=$(get_tag_with_fallback "$1" "$2" "$3")
  docker_image="$ecr_url/$component:$tag"
  echo -n "$docker_image"
}

read_secrets() {
  if [ -f "$HOME/.secrets.tfvars" ]; then
    echo -n "-var-file=$HOME/.secrets.tfvars"
  else
    echo -n ""
  fi
}
