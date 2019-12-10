Readme for infrastructure as code
=======
### Context
A working CVP environment consists of
* a POSTGRES database
* a VLAN (a _virtual private cloud_ in AWS-speak)
* Credentials verification services, such as connector and node.
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
* Install the AWS cli and configure it using `aws configure` to use up the credentials created above.
* Setup MFA on your AWS account at https://console.aws.amazon.com/iam/home?region=us-east-2#/security_credentials.
* Install ‘awslogs’ from https://github.com/jorgebastida/awslogs. This is optional and enables the `env.sh -w` flag which
  will show logs from an AWS environment directly in your terminal window).
* Install jq (https://stedolan.github.io/jq/).
* Install graphviz from https://graphviz.gitlab.io/. This is optional and enables the `env.sh -g` flag which
  produces a diagrammatic hierarchy for your environment.
* Install 'terraform' from https://www.terraform.io/downloads.html.
* Install dbeaver. To connect to the RDS database instance, you need to create a tunnel, for e.g. You are then able
  to connect to the RDS database using `localhost:5432` as the host.
```bash
# substitute the correct IP address for an EC2 instance inside the VPC.
ssh -L5432:credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com:5432 -i ~/.ssh/id_rsa ec2-user@3.133.101.108
```
* The infra code does not currently create or destroy databases. If you do want to create a new database for your environment
  (e.g. to experiment with new migrations) then run:
```postgresql
CREATE DATABASE connector_<unique suffix>;
CREATE USER connector_<unique suffix> WITH ENCRYPTED PASSWORD ‘some password’;
GRANT ALL PRIVILEGES ON DATABASE connector_<unique suffix> TO connector_<unique suffix>;

CREATE DATABASE geud_node_<unique suffix>;
CREATE USER geud_node_<unique suffix> WITH ENCRYPTED PASSWORD 'some password';
GRANT ALL PRIVILEGES ON DATABASE geud_node_<unique suffix> TO geud_node_<unique suffix>;

```

### Changing either the docker image or database used by an environment
The root build file now contains targets to build and push a docker image to Amazon ECR (a docker repository).
* `mill connector.docker.build && mill connector.docker.push` (note there are currently some dependency glitches which may
   require a `mill clean` to ensure your changes are pushed). The docker image that gets pushed used the git sha-1 hash
   (as output from `git rev-parse HEAD`) to tag the image.
* To change the docker image deployed by terraform, edit env.sh inside the `write_vars`, substituting the image URI you require
  (yes, this is horribly manual and we need a convention to automate this).
* You can also change the database and username inside env.sh at `write_vars`.

### Secrets management
Create a file called `~/.secrets.tfvars` with the following:
```bash
$ cat ~/.secrets.tfvars
connector_psql_password = "<secret>"
node_psql_password = "<secret>"
bitcoind_password = "<secret>"
```

### Environment lifecycle
The basic use-case for creating, working with and destroying an environment is as follows
```bash
# Use -p to check syntax etc before making an changes on AWS
./env.sh -p myenv

# Use -a to apply changes to an environment. This creates the env on AWS.
# After running, terraform will output the hostname you can use for gRPC requests.
./env.sh -a myenv

# Watch the logs from the environment
./env.sh -w myenv

# Drop the environment
./env.sh -d myenv
```

### Gaining access to the environments
You can SSH into EC2 instances inside an environment you create as follows:
* Navigate to the list of ECS clusters and find the one corresponding to your env at https://us-east-2.console.aws.amazon.com/ecs/home?region=us-east-2#/clusters  
* Drill down into your cluster name and then select the 'ECS Instances' tab (this will contain a table, with an 'EC2 instance' column).
* Select the relevant EC2 instance and copy its 'IPv4 Public IP' value.
* `ssh -i ~/.ssh/id_rsa ec2-user@<ip obtained above>` (note the username!).

### Terraform state files
Assuming you have used the `env.sh` script, terraform will store what it thinks is the current state of a given environment
in a directory called `.<env name>`.

If you have dropped the environment with `env.sh -d <env name>`, you may delete the directory containing the terraform state.
*Do not delete this directory* while resources from the environment still exist. If you do, terraform will lose track of
resources on AWS and you will have to find and delete them manually.

TODO: Store terraform state in S3. If you want to change an environment that is
shared and not personal to you, *remember that you will need to obtain the most up to date state file from the person
who last changed the environment.

### Reminder
Environments on AWS cost money. Avoid creating loads of environments then forgetting about them and
if you create an environment and are not going to be using it for more than a few days then
destroy it with `env.sh -d <my env>`.
