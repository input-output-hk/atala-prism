#!/bin/bash

yum install -y awslogs

cat << EOF >> /etc/awslogs/awslogs.conf
[general]
state_file = /var/lib/awslogs/agent-state

[/var/log/dmesg]
file = /var/log/dmesg
log_group_name = /var/log/dmesg
log_stream_name = ${ecs-cluster-name}-{instance_id}

[/var/log/messages]
file = /var/log/messages
log_group_name = /var/log/messages
log_stream_name = ${ecs-cluster-name}-{instance_id}
datetime_format = %b %d %H:%M:%S

[/var/log/docker]
file = /var/log/docker
log_group_name = /var/log/docker
log_stream_name = ${ecs-cluster-name}-{instance_id}
datetime_format = %Y-%m-%dT%H:%M:%S.%f

[/var/log/ecs/ecs-init.log]
file = /var/log/ecs/ecs-init.log
log_group_name = /var/log/ecs/ecs-init.log
log_stream_name = ${ecs-cluster-name}-{instance_id}
datetime_format = %Y-%m-%dT%H:%M:%SZ

[/var/log/ecs/ecs-agent.log]
file = /var/log/ecs/ecs-agent.log.*
log_group_name = /var/log/ecs/ecs-agent.log
log_stream_name = ${ecs-cluster-name}-{instance_id}
datetime_format = %Y-%m-%dT%H:%M:%SZ

[/var/log/ecs/audit.log]
file = /var/log/ecs/audit.log.*
log_group_name = /var/log/ecs/audit.log
log_stream_name = ${ecs-cluster-name}-{instance_id}
datetime_format = %Y-%m-%dT%H:%M:%SZ
EOF

sed -i 's:^region *=.*$:region = ${aws-region}:' /etc/awslogs/awscli.conf

# See https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_cloudwatch_logs.html
sudo yum update -y ecs-init
echo ECS_CLUSTER=${ecs-cluster-name} > /etc/ecs/ecs.config

systemctl enable awslogsd.service
systemctl restart awslogsd.service
systemctl restart docker
