Readme for infrastructure as code
=======

### Context

A working PRISM environment consists of
* a POSTGRES database
* a VLAN (a _virtual private cloud_ in AWS-speak)
* Credentials verification services, such as connector and node.
* Supporting services, like envoy.
* Services supporting the development workflow, such as the ECR docker repository.

Currently, we have code to provision
* the VLAN
* the service tier

We have manually provisioned an RDS database instance for a test environment. We should consider creating code for
this (perhaps adding it to the `stage/base` directory).

We have also manually provisioned an _atala_ ECR repository. We can consider this a one-off activity so there is little
need to code this.

### Architecture

Services (e.g. intdemo) are deployed into Virtual LAN network - VPC in AWS nomenclature. All testing services - ones built from `develop` branch or feature branches should go into one network. That means that networks and services have different lifetime cycles.

VPC is generally deployed once and then only modified when needed. Default network's name is `prism-test`. Each network should have an associated private DNS namespace for [Service Discovery](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-discovery.html), default defined as `<vpc name>.atala.local`. Another component that comes with a network is monitoring: [Prometheus](https://prometheus.io/) as time-series database backend and [Grafana](https://grafana.com/) as dashboards UI. Apart from infrastructural services, there is also a shared Cardano Wallet deployed, so that it doesn't need to be re-synced on each new environment. All such services are defined in `base` subdirectory.

Services in staging/testing tier have much shorter lifetimes as they are redeployed after each build. It is responsibility of service deployment to spin up its load balancers (Envoy / AWS LB), set up DNS addresses, etc. Metrics exposed can be automatically discovered by monitoring corresponding to the VPC.

#### Networking

All development instances are deployed to the same VPC/VLAN defined in `stage/base/vpc`. It contains one public and one private subnet in two availability zones with NATs assinged for private ones. Identifiers and IP ranges used in services should not be hardcoded and should be loaded from the state stored in the bucket instead.

#### TLS support

TLS support is provided by AWS Elastic Load Balancer. TLS is terminated there and all traffic is routed unsecured to components in private network. TLS certificates are stored in AWS Certificate Manager.

#### Monitoring

Metrics from instances are collected using Prometheus instance and can be viewed using Grafana. There are two metrics sources:

* EC2 instance metrics are exposed by Prometheus node exporter running on the instance on port 9100 and detected using EC2 service discovery - the instance need to be in the same VPC and have `Prometheus` instance tag set to `true` and node exporter installed on initialization.
* Envoy metrics are exposed and discovered using [ECS discovery Python script](https://github.com/signal-ai/prometheus-ecs-sd) running on Prometheus instance. ECS services need to have environment variables set as [explained in the documentation](https://github.com/signal-ai/prometheus-ecs-sd#networking).

#### Services

Services running in the cluster consist of one Envoy ECS task serving as loadbalancer and one or more ECS tasks with implementations and supporting services. Service instances use `awsvpc` networking mode under with private subnet and are discoverable using DNS-based server discovery.

As Envoy needs to configure listeners corresponding to ports used by services, each service needs its own entry in Envoy configuration. Connector and Node components have their own listeners, other use path-based routing.

### Usage

#### Instance access

All development EC2 instances should include SSH keys of team members no allow them logging in. To make management easier they are provided by `modules/ssh_keys` terraform module. If you want to gain access, add your key there.

ECS clusters are managed by AWS (Fargate) while Prometheus instance is running on Ubuntu with `ubuntu` as the username.

### Possible improvements

For now all data is transported over unsecured HTTP protocol. TLS keys and certificates should be added to Envoy in order secure the communication before going to production.

#### Config files on S3

Currently configuration files are deployed to EC2 instances using AWS user data shell script which has size limit of 16kB. The only configuration we can provide to ECS tasks in environment variables.

Provisioning configuration files into S3 bucket and downloading them on startup would make configuration more robust and allow e.g. using single Envoy docker image, providing configuration from the outside.

###  Directory overview

There are following top level directories:

* `modules` contains common, reusable definitions that can be used to create environments,
* `stage` containts definitions of environments used for testing, generally deployed by the CD/CI pipeline,
* in future `prod` directory is going to contain definitions of production environments.

`modules` directory contains following subfolders:

* `ssh_keys` - definition containing `authorized_keys` file with SSH keys of team members to use in machine deployments,
* `services` - definitions of services, collection of components deployed together, working as a bigger whole,
* `components` - definitions of comonents, elements of services, that are generally not deployed alone.

Tier directories (`stage`, in future `prod`) contain following subfolders:

* `base` - supporting architecture, e.g. VPC creation, Prometheus setup, etc.
* `services` - instantiations of services.

Each of tier subfolders contains shell script with matching name used to managing the deployment.

There is also a deprecated directory for the old infrastructure:

* vpc - this sets up a base lan into which application environments can be added. For example, there could be a test vlan
  with environments called 'alex', 'ata1650', 'jmt', 'uitest'. To get started look at the usage of `vpc.sh`.
  Currently, several values output from the vpc setup are hardcoded in the services definitions below. Bear this in mind
  if you do use the vpc script and want to then create environments inside your new vpc.

### Some setup
There are few setup steps if you wish to create and work with your own testing environment. Here is an overview:
* Create an AWS access key for your AWS account at https://console.aws.amazon.com/iam/home?region=us-east-2#/security_credentials.
* Install the AWS cli and configure it using `aws configure` to use up the credentials created above (be sure to specify the region `us-east-2`, otherwise, the scripts will fail).
* Setup MFA on your AWS account at https://console.aws.amazon.com/iam/home?region=us-east-2#/security_credentials.
* Optional: Install ‘awslogs’ from https://github.com/jorgebastida/awslogs. This enables the `prism.sh -w` flag which
  will show logs from an AWS environment directly in your terminal window).
* Install jq (https://stedolan.github.io/jq/).
* Optional: Install graphviz from https://graphviz.gitlab.io/. This enables the `prism.sh -g` flag which
  produces a diagrammatic hierarchy for your environment.
* Install 'terraform' from https://www.terraform.io/downloads.html. It needs to be in proper version, the easiest way to find is to run:
```
cat ./stage/services/prism/versions.tf
```
* Optional: Install dbeaver. To connect to the RDS test database instance, the use the following hostname: credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432
* If you want to access the EC2 instances (e.g. to examine the output of `docker ps`), you should
 * Go to the aws console, find the ECS cluster and drill down to the EC2 instances. Copy-and-paste the public IP.
 * Login as _ec2-user_. i.e. `ssh -i ~/.ssh/<my key> ec2-user@<public ip>`
* You will need to have the `PGPASSWORD` with the RDS postgres password, otherwise, the scripts will fail, ask someone from the team to share the password and run `export PGPASSWORD=thepassword` before running the `prism.sh` script.

### Changing either the docker image or database used by an environment
Circleci will automatically build and push docker images for the PRISM components. If you want to do this manually, the root `build.sc` 
file now contains targets to do this.
* `sbt connector/dockerBuildAndPush`. The docker image that gets pushed will have a tag with the following format: `<branch name prefix>-<revision count>-<sha>`.
* The prism.sh script, which you should use to invoke terraform, understands this convention. It will try to configure your environment in the following way:
 * If, for a given cvp component, there is a docker image for your branch, it will use the latest image.
 * If there is none, it will fall back to the latest image built from the develop branch.
* The rationale here is that if you are working on a branch and, say, only change the connector, the env script will deploy
  the connector component from your branch and everything else from develop. In theory, then, your branch should be properly
  integration tested with other develop components by the time you merge down.  

### Secrets management
Create a file called `~/.secrets.tfvars` with the following:
```bash
$ cat ~/.secrets.tfvars
postgres_password = "<secret>"
bitcoind_password = "<secret>"
```

**NOTE**: While `postgres_password` is not required for development, it's required for CircleCi, just place any value.

### Environment lifecycle
The basic use-case for creating, working with and destroying an environment is as follows

**NOTE**: Running the deployment scripts infers the environment name to be the current branch, most of the time, you'll want to specify a name, for example `./prism.sh -A my-test-env` will update the environment named `my-test-env`.

```bash
# Optionally, use -p to check syntax etc before making an changes on AWS
./prism.sh -p

# Use -a or -A to apply changes to an environment. This creates the env on AWS.
# After running, terraform will output the hostname you can use for gRPC requests.
./prism.sh -a  # use prism.sh -A to apply without prompting for approval.

# Show information about an environment, including it AWS resources, its URLs and node/connector DB credentials
./prism.sh -s

# Watch the logs from the environment
./prism.sh -w

# Drop the environment
./prism.sh -d myenv  # use -D to drop without prompting for approval

# To modify/query a different environment to your branch, specify the environment name explicitly as the last arg to prism.sh
./prism.sh -s 

# To update the default aws capacity provider from FARGATE_SPOT to FARGATE 
./prism.sh -pF or ./prism.sh -aF or ./prism.sh -AF
```

### Automatic environment builds
Circleci will create docker images and an AWS environment for pushes to branches with names matching `develop*`, `qa*`, `sandbox*`, `demo`, `test*`.
For any other branch name, it will do nothing infra related (no docker images, no AWS env).
If you want to test a story on AWS, before merging to develop, you should merge your changes into branch `test-ata-9876` and push.

Components that are crucial (`connector`, `node`) are deployed always, other are deployed selectively, for lowering costs. In case of some branches the set is fixed: all components for `develop`, management console for `demo`, management console + intdemo for `qa` and `sandbox`. In case of `test*` branch only components deployed are the ones contained in branch name (available ones: `geud`, `intdemo`, `mirror`, `kycbridge`). For example `test-mirror-intdemo-ata-1234` will deploy Mirror and Intdemo components.

### Terraform conventions

#### State

All state generated by deployments needs to be stored in the cloud, in current setup it being S3 buckets. The bucket is always `atala-cvp` and the key follows directory structure with the name of the deployment added as the last level of directories and `terraform.tfstate` as the filename. For example state for definitions in `infra/stage/services/intdemo` for `develop` deployment will be stored at `s3://atala-cvp/infra/stage/services/intdemo/develop/terraform.tfstate`.

If you have dropped the environment e.g. with `prism.sh -d <env name>`, you may delete the directory containing the terraform state.
*Do not delete this directory* while resources from the environment still exist. If you do, terraform will lose track of
resources on AWS and you will have to find and delete them manually.

#### Naming conventions

* identifiers of Terraform variables, locals, and resource use lower case letters: `variable_name`,
* labels and names sent to AWS should follow AWS naming conventions, most often being it `instance-name` or `security-group-name`.

#### Directory structure

Each deployment definition contains `variables.tf` with input variables definitions, `outputs.tf` with output definitions, and `main.tf` with resource definitions. If there are many definitions, some of them can be extracted into other `.tf` files.


### Reminder
Environments on AWS cost money. Avoid creating loads of environments then forgetting about them and,
if you create an environment and are not going to be using it for more than a few days, then
destroy it with `prism.sh -d <my env>`.
