# Integrating Cardano with CircleCI pipelines

There are two axes of integrating Cardano components:

1. Ephemeral, short running instance vs shared, long running one: either we launch new instances on each build or we connect to instances configured in AWS cloud, shared between old builds.

2. Testnet vs separated one-node network: either we connect to the existing Shelley network.

One of combinations - ephemeral testnet node - would be very slow, as it would need to synchronize whole history and it takes time. Another - long running one-node network setup - is harder to configure that the testnet one, while not giving us much more benefits. That leaves us with two solutions: long-running testnet setup and ephemeral single-node ones.

## Dockers in CircleCI

Cardano components are provided as separate Docker images. They are not independent: in order to run, cardano-wallet and cardano-db-sync need a shared Unix Socket on mounted volumes. That appears to be hard in CircleCI setup.

CircleCI pipelines can run Docker images three ways:

1. Directly by the docker executor of the job. Such containers are configured in `config.yml` file and are accessible from the job. The downside is that [it is not possible to attach volumes to the containers](https://circleci.com/docs/2.0/executor-types/#docker-benefits-and-limitations) in such setup.

2. Using mechanism called [remote docker environment](https://circleci.com/docs/2.0/building-docker-images). It allows launching new containers using `docker` command [or even `docker-compose`](https://circleci.com/docs/2.0/docker-compose/#using-docker-compose-with-docker-executor) and mounting volumes (I'm not 100% confident if shared volumes though). The problem is that programs executed directly by the job [cannot communicate with the remote docker containers](https://circleci.com/docs/2.0/building-docker-images/#accessing-services).

3. Using [`machine` executor](https://circleci.com/docs/2.0/executor-types/#using-machine). Instead of running jobs in a docker container with tools preinstalled in the image, we would run them in a fresh Linux environment. We could just run docker container with the tools, but we won't be able to control it via `config.yml`. Another downside is longer startup time, see [the table](https://circleci.com/docs/2.0/executor-types/#docker-benefits-and-limitations).

That means that in order to run new node on each CircleCI build, we would have to - accordingly:

1. Use Nix to build shared Docker image, containing cardano-node, cardano-wallet, and cardano-db-sync together, so no volumes are required. People on Slack [said it should be possible](https://input-output-rnd.slack.com/archives/C819S481Y/p1590673096299300), but probably would require getting more familiar with Nix.

2. Dockerize Scala tests - i.e. build a docker image with Prism Node running inside, run it in the remote environment and then copy the results back to the main process to display them.

3. Modify the process to run on `machine` executor, making it even slower.

## Choice

As explained in the previous section, starting new set of Cardano services for each build would require much work. Instead long-running cardano-node, cardano-wallet, and cardano-db-sync have been deployed on AWS, using Terraform. CircleCI reads the parameters (such as PostgreSQL credentials or wallet API IP) from the Terraform state, while wallet specific parameters (wallet ID and password) are stored in [CircleCI environment variables](https://circleci.com/gh/input-output-hk/cardano-enterprise/edit#env-vars).

If such choice makes integration testing harder, we can alway explore other possibilities, weighting between using some workarounds in the tests and making the deploment more complex.

## Managing the services

In order to access services you should SSH to the machine running them (your key needs to be present in `infra/modules/ssh_keys/authorized_keys`):

```
$ cd prism-backend/infra/stage/base/cardano
$ ./cardano.sh -s | grep cardano_instance_public_ip
cardano_instance_public_ip = "3.15.237.14"
$ ssh ubuntu@3.15.237.14
```

* If you cannot connect to the Cardano server on AWS, you may want to reboot it from the AWS Console [here](https://us-east-2.console.aws.amazon.com/ec2/v2/home?region=us-east-2#InstanceDetails:instanceId=i-0f4f60f51dca39179).
  If you do so, the `/data` directory will no longer exist as it will need to be mounted manually, but you can mount it with:
```
sudo mkdir -p /data
sudo mount /dev/xvdh /data || (mkfs.ext4 /dev/xvdh && mount /dev/xvdh /data)
```

Services are deployed using docker-compose definition in `/cardano/docker-compose.yml`, configured as a systemd service. You can query its state (e.g. `systemctl status cardano`), you can see its logs via journalctl (e.g. `journalctl -u cardano --since today`).

You can also check docker containers directly, e.g. `sudo docker ps`, `sudo docker logs [container id]`, `sudo docker exec [container id] bash`.

Wallet can be managed as explained in [run-cardano.md](run-cardano.md) file. Setting up wallets is done manually, not based on Terraform.

## Relevant discussions on Slack

During work on that task I have asked people from other teams for help, here are some threads:

* [Discussion on building monolithic docker image, containing cardano-node, cardano-wallet, and cardano-db-sync](https://input-output-rnd.slack.com/archives/C819S481Y/p1590673096299300)

* [Discussion on configuring one-node Shelley network, including configuration from the wallet team](https://input-output-rnd.slack.com/archives/C819S481Y/p1590674954308100)
