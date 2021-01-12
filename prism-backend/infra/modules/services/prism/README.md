# PRISM service module

Deploys Credentials Demo Website as ECS tasks cluster, together with Envoy loadbalancer. It requires previous ECS cluster deployment, which can be done using `ecs_cluster` module.

ECS tasks use `awsvpc` networking mode, which means that the task (consisting of many containers) gets its own IP which is distinct from EC2 machine it is running on and can be used to access it directly. That allows Envoy to discover tasks using DNS.

Envoy itself is deployed using `bridge` networking mode and can be accessed using AWS builtin TCP-based loadbalancer (ELB).
