Readme for infrastructure as code
=======
### Context
A working CVP environment consists of
* a POSTGRES database
* a VLAN (a _virtual private cloud_ in AWS-speak)
* Credentials verification services, such as connector and node.
* Supporting services, like bitcoind.
* Services supporting the development workflow, such as the ECR docker repository.

Currently, we have code to provision
* the VLAN
* the service tier

We have manually provisioned an RDS database instance for a test environment. We should consider creating code for
this (perhaps adding it to the `vpc` directory).

We have also manually provisioned an _atala_ ECR repository. We can consider this a one-off activity so there is little
need to code this.

###  Directory overview
There are two directories
* vpc - this sets up a base lan into which application environments can be added. For example, there could be a test vlan
  with environments called 'alex', 'ata1650', 'jmt', 'uitest'. To get started look at the usage of `vpc.sh`.
  Currently, several values output from the vpc setup are hardcoded in the services definitions below. Bear this in mind
  if you do use the vpc script and want to then create environments inside your new vpc.
* services - this sets up docker containers for the credentials services (using amazon ECS, which is a provisioning service
  for docker).

### Some setup
There are few setup steps if you wish to create and work with your own testing environment. Here is an overview:
* Create an AWS access key for your AWS account at https://console.aws.amazon.com/iam/home?region=us-east-2#/security_credentials.
* Install the AWS cli and configure it using `aws configure` to use up the credentials created above (be sure to specify the region `us-east-2`, otherwise, the scripts will fail).
* Setup MFA on your AWS account at https://console.aws.amazon.com/iam/home?region=us-east-2#/security_credentials.
* Install ‘awslogs’ from https://github.com/jorgebastida/awslogs. This is optional and enables the `env.sh -w` flag which
  will show logs from an AWS environment directly in your terminal window).
* Install jq (https://stedolan.github.io/jq/).
* Install graphviz from https://graphviz.gitlab.io/. This is optional and enables the `env.sh -g` flag which
  produces a diagrammatic hierarchy for your environment.
* Install 'terraform' from https://www.terraform.io/downloads.html.
* Install dbeaver. To connect to the RDS test database instance, the use the following hostname: credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432
* If you want to access the EC2 instances (e.g. to examine the output of `docker ps`), you should
 * Go to the aws console, find the ECS cluster and drill down to the EC2 instances. Copy-and-paste the public IP.
 * Login as _ec2-user_. i.e. `ssh -i ~/.ssh/<my key> ec2-user@<public ip>`
* You will need to have the `PGPASSWORD` with the RDS postgres password, otherwise, the scripts will fail, ask someone from the team to share the password and run `export PGPASSWORD=thepassword` before running the `env.sh` script.

### Changing either the docker image or database used by an environment
Circleci will automatically build and push docker images for the CVP components. If you want to do this manuall, the root `build.sc` 
file now contains targets to do this.
* `mill connector.docker-build && mill connector.docker-push`. The docker image that gets pushed will have a tag with the following format: `<branch name prefix>-<revision count>-<sha>`.
* The env.sh script, which you should use to invoke terraform, understands this convention. It will try to configure your environment in the following way:
 * If, for a given cvp component, there is a docker image for your branch, it will use the latest image.
 * If there is none, it will fall back to the latest image built from the develop branch.
* The rationale here is that if you are working on a branch and, say, only change the connector, the env script will deploy
  the connector component from your branch and everything else from develop. In theory, then, your branch should be properly
  integration tested with other develop components by the time you merge down.  

### Secrets management
Create a file called `~/.secrets.tfvars` with the following:
```bash
$ cat ~/.secrets.tfvars
bitcoind_password = "<secret>"
```

### Environment lifecycle
The basic use-case for creating, working with and destroying an environment is as follows

**NOTE**: Running the `./env.sh` scripts infers the environment name to be the current branch, most of the time, you'll want to specify a name, for example `./env.sh -A my-test-env` will update the environment named `my-test-env`.

```bash
# Optionally, use -p to check syntax etc before making an changes on AWS
./env.sh -p

# Use -a or -A to apply changes to an environment. This creates the env on AWS.
# After running, terraform will output the hostname you can use for gRPC requests.
./env.sh -a  # use env.sh -A to apply without prompting for approval.

# Show information about an environment, including it AWS resources, its URLs and node/connector DB credentials
./env.sh -s

# Watch the logs from the environment
./env.sh -w

# Drop the environment
./env.sh -d myenv  # use -D to drop without prompting for approval

# To modify/query a different environment to your branch, specify the environment name explicitly as the last arg to env.sh
./env.sh -s 
```

### Gaining access to the environments
You can SSH into EC2 instances inside an environment you create as follows:
* Navigate to the list of ECS clusters and find the one corresponding to your env at https://us-east-2.console.aws.amazon.com/ecs/home?region=us-east-2#/clusters  
* Drill down into your cluster name and then select the 'ECS Instances' tab (this will contain a table, with an 'EC2 instance' column).
* Select the relevant EC2 instance and copy its 'IPv4 Public IP' value.
* `ssh -i ~/.ssh/id_rsa ec2-user@<ip obtained above>` (note the username!).

### Terraform state files
Assuming you have used the `env.sh` script, terraform will store what it thinks is the current state of a given environment in S3
under s3://atala-cvp/infra/services/<env name>.

If you have dropped the environment with `env.sh -d <env name>`, you may delete the directory containing the terraform state.
*Do not delete this directory* while resources from the environment still exist. If you do, terraform will lose track of
resources on AWS and you will have to find and delete them manually.

TODO: Store terraform state for the VPC build in S3. Currently this still uses local state storage.

### Automatic environment builds
Circleci will create docker images and an AWS environment for pushes to branches with names matching `develop`, `infra*` and `test*`.
For any other branch name, it will do nothing infra related (no docker images, no AWS env).
If you want to test a story on AWS, before merging to develop, you should merge your changes into branch `infra1` and push. 
If somebody else is already working with `infra1`, use branch `infra2`, etc. The same is true for any branch named `test*`.

### Reminder
Environments on AWS cost money. Avoid creating loads of environments then forgetting about them and,
if you create an environment and are not going to be using it for more than a few days, then
destroy it with `env.sh -d <my env>`.
