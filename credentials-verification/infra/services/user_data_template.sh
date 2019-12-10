#!/bin/bash
set -e

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

cat << EOF >> /home/ec2-user/.ssh/authorized_keys
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlWTTPD0dS5ihB3vjG2PhEpMw8V4Zf8f5ve7CwxNRqPQiEZR0/ABhOA3FwnaMLhBJjJPdrD23Uwk9R/FfObtYkBj9Areq4b7zb2zA84Kd/Pe8+rIk1OWbC2mrhJR0FPmgWnwR8CQtGyzCCMwM/rVADbEuGUypEN2BgJ508DE/A7NfiETLGTXVfhqIIjhpPWT8qr1zrFJI1vDG/ic1mDhmKy8O8rhLtNnGv3SYIIy4qbJeEHvCoPVDkuJM4HIW4VZYdC9qakh6Qm/AsWkdFZc/75XNuYObozv608O/aDZDNzN04/dgRjX/RLBtF3ktm6b89djagR4EHMF143iqrEHOD jtownson@MacBook-Pro
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDBoqAVlOtr/REZeDauOjIVHV3Jwgy61fOVnBVQ+ccMhqze0EGabprn0+zQvIwgHTVNHk9N9nlFHZIQoFzD5RjNyRdF8WvPjIRkzAJxf4mq5pfTzWWB2feXNnQM7nolWCmGnPdWky/LL7e+ogP3RrR3tBtILE1ZsSVV2CYdRgOm8Nt3+YUOoqK2hcpaigFgmOd94CVD0/Gs96X/qC/XJBlFueV2BS1CzqhR8hanQAzZFQ0YomqRpuq7namY6DFfvFEOH0RKtjsrR3bzXM/t+5EfSthnUQKDEGCqjdpvqQlVKGp6JBtMgM3nE9wgYpduFUxWz/RbBCFtWymCYn0UvkIb5MWBLwAO5MQ6fkEWG3m7huJxwOnRRdOE9MONkx/u2RebLlCtrr4+ld80EvwGPlIXXKyFB1Riif3wkZJFY3hltMBOoYzx2NffKnEuk+k6wyh2DQfdvyVFdw0R1IZZoA2HamvI+txRjrhCedD4bLLCBV7vyW3TPhYQm4VX8lFr+8VclUY6fTCwsjTUQKfFXcjq48igf/azJ5dEgq507dd+EJ7mggRKGplcdb31AKIvjyyD/yJkb7IkbqU7NRq2lcjFcgFKrQUVCX06CPGwyhT/OFbt+fi3lYmY57jqrP+Avo2fqHfYUDb8aSyjIALl78iBF4GqZGmOjokAK4ZZkrI62Q== alexis22229@gmail.com
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDCEK8vMVgMveJQLGYISeagS6OxZ4xB/IUI5TBPKNsGoNjItIRIDt3VOo2rWjNv9Vumc+lsSXVsmFQkBE+BW1wyOEZfn4j5z2fi75G7P4F1pzVn72hmZ00R24F6AFboGMb5r6LtrXlnywjFWNFCh/jYaOwtV//rdvGylqaMGz+A80ljnUv7jJfJ4tA0Amc8A0CH/vowZzonaBu9iCPaKt2DD5wSf7TU0a0Yzib17E4Z0MM/ToPoSH2nVBVN3mf6+fOn1yFvxJx5SVgVwTAAhR17Jo/ajmzQ5XbDQKfMST/ZG7XgphCgXjuS6A9AvwTwtqJndVxpBhSP6nmwkIEZ8sFV8NMgodI97s5iotq5eWKwedp6ayZc1+ApEoAkVZIxV1NDnl997GgIyFM9NvlFcxMoy9suN6TFFeXl+w87LiiVlmrsAPiQCioeYlPsIEjH6/N0PbNBFjnmbwEVz5+5TDGAPYdYk/OfolJrtvWISTJbAxMvzQM7tKLcKIHZJYOIv9HCb474Okx48b1G0ErWK6LPzUKvDQHDZJ3QLpPUERmJnIDL6h3SaRDZTUhnJ4taGeNP4i6IucYpFQZEKurDbNd6XdrdZgQoDrHgLPQttU0O141s4DtJl8RkCref7Mm6RsTITituMSgHc9LQnUP+x/yQkBTG5MjUzAkpvMNdxU6Gqw== jaacek.kurkowski@gmail.com
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAEAQDtKsiPQvoPuATmjU7v/7FgByp9OgfolvX0Dq1+L0s1zAJbBPpolFThNbnw8czE6shEtLsVMVR9prlAJxAvgCn45SgzfuiskLZp0kfhyybkgGVFMG92BmmAKYqBbSllfnTio9nVzD+NOZMbPIYIc0A40KC+dptRxOfscAe6g/H6FMj3ymqyuifKLCMHK6A8IMtFbuBi/QII2XfycA2naYG4tbrhYMTs98HwsiraqMTE72ex0E6JNVuTzeOG2Zn8o9c6klWI1WOp0aPVFKKSAzPSkLORPPOor1H2+XP+zyfwy4mX15Be7ZA9l4r9AQSnxgal/Xy+Tn/t1zYcxXFfzmlUBWsWg27Mxd/DgiAgagqo9Wcmcfv48PhSXEQjeHJYuQNcX9meDx5FH7CYJz6NmJVUS0LOyHadCubRjZJ+Ddem+Y0V7xFi94p3YEcLYROesC6Oiy9Sp6Qm2zshu/Ohh6PSkMaJ65ssW9c+Exkil017kXTAzSjw0xHPM1KM+0uG2XTM+fm8aB35F8gzTMDjvigoB4DcgHSq2uj2/ywkzYkFPQvjePIBuBzDI0y0EAfkBO7hYLQcw+uOMqoaXRUj9DDz3Wf7h71ukvh/+/QAuOh/uiDdaRSeBUqJkpPfxv+J2hdc6nyrGZqaYDBBRdE9GIo+3QXxAvInAd4K8ZCZyOHMj0i6/KYOwIwgksnn2+1nQFtQ582AdpOVKYFmrmU61goUFMYS4BMR+YZtETOjMY2vCi2HEt1BHofhn0uxGYb42d67tSTvQVnrVuX4Z1to03d9tS86gBIFxlVkw8L2+QYTAMscBYAooPRSaXuT5DL30/UpjEAfdaWao137w+l6+v8mED71UneIWAwYqyZrJI5irPBofKLc2KrlzAUXdbbrd0okgHWnFXTe+1A6iqKJpt0eAW8VxRTNDrt6Pc4F0nuihypTeYrxpujxTYVp0Fbf/epW10R7TnU0V9AmbPMKtqGKLeKV1c0JiezctB3yu6GSb99aTGw/Rta2Oj3C09A9d652a5uKXHruwtXFrui4f/7f/iveIFBUy3habosPDRSdjJGNOQy4DObQHgYa/ciQPwUFGgXm3B4J7XhtQjQeMcgJuKW56jUhkFkReRbuS42xnUReG/tH/b6GXQnAdvgmzBE4jIVq9wHwfuQyenRtS+/lGPzeiQYkhf1yKaBjwMYcgHlo6Vggbvqaus63fuig2iu99b/jepKLItc8GCxKMHOc63C80rzQVrtu55gLZhiB9cAQ4HAdyQGWm75gMf29fXeTx7qsyCTfUXfqKfsqmFF/qyYHt/AfQb5acx8KztQ+1vZpRrBTIeO7RH7RUgPB7MweEpP1/PHiik1JMaMtd461 christos.loverdos@iohk.io
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQCdE+KSLfK/lQ9HwNqUO5yoi8g1aKGkOtWGif2qLY0UZuNt8PemzooJ3/8j647G676lKDiGkx4J7vAkWA/dIxI8LGjVUfTw+DqA/cwtRcqd8f5mWr2I9nFCJh1uO5gceXFk5BrYVP2L9FK+48dN1ZPurmDdTJ+xL4JxoDyqRkbjcWmcOo+Gfkv3ZZV3YqvYMvMn0u1+rseoZ+MKfD3M6gGU1bHGAfimLEv4BmijUhhyTkhWtz2XprE4291bAaQt7Ak0zZKzyJ+2RIglR7RZDEYqgGvBlzyvdhaMdB/2/O2480NPJNu67sg1NMYQohMc7PBhLy1bJg0RFL+D0q45EC6WOKqr65M9zOH5hKXacOUQOVuNK8siu9vnqoqIKEzm0qAQY0pMFZYWNTYpcxs3l0VBXDSJKz6gpft7yQmbx1VnmyJcyCJ8GGzDfa+qpby4G4zdkyA8BXY3Fh4W5QE8BuopmNngBNyRN1MAAvfaqK3N81cMg/8PtQWt5p+x4ONNJA1RLcQ1w23mTf+LTZEIaokwt8jfbWDibQfXf+3kGwKRYP2ZIg01bCza1NXzsL46GrpND/Vantuh8hcBrdsfGsaYHE1XSx4gJfDUt8OkY75M3rSWXLg+RThXISwnSxzoj67gQKo9pIYwDigW5JpKZSsDd3uU9/ZTDKWiqBNUysk73w== shailesh.patil@iohk.io
EOF

chown ec2-user /home/ec2-user/.ssh/authorized_keys
chmod 600 /home/ec2-user/.ssh/authorized_keys

# See https://docs.aws.amazon.com/AmazonECS/latest/developerguide/using_cloudwatch_logs.html
sudo yum update -y ecs-init
echo ECS_CLUSTER=${ecs-cluster-name} > /etc/ecs/ecs.config

systemctl enable awslogsd.service
systemctl restart awslogsd.service
systemctl restart docker
