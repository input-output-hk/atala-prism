#!/bin/bash

# usage: restart.sh <ecs cluster name>
# Stop all services in the cluster.
# ECS will immediately restart them.
# Useful for forcing updates.
taskArns=$(aws ecs list-tasks --cluster "$1" --query "taskArns[*]" --output text)

for taskArn in $taskArns; do
  aws ecs stop-task --cluster "$1" --task "$taskArn"
done
