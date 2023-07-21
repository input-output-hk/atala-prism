BASIC AUTH
===
This base setup is part to publish auth lambda
* Publishes basic auth lambda in the region us-east-1.
* an AWS lambda edge is used as a trigger with cloudfront to provide basic auth protection.
* This is one time task only once lambda is published it just reerenced with its arn
The auth.sh shell script works in the same way as other infra scripts.
Type `./auth.sh -h` to get started.
