## General info

This terraform setup deploys following components:

* `cardano-node` - full Cardano node,

* `db-sync` - component pushing information on transactions and blocks to PostgreSQL database,

* `cardano-wallet` - wallet backend that can be used to create and publish transactions.

Deployed machine is configured with permanent volume which stores all the data, mounted at `/data` - don't worry about stopping the container and starting it again. Just make sure the volume is not destroyed - but Terraform shouldn't let you do that.

While software provisioning is done automatically using Terraform, the wallet for Cardano has been created manually and is managed that way. **Up-to-date wallet parameters are kept in `/data/wallet-parameters` file - please keep it that way.**

## Upgrading

When upgrading it's better to first do it manually, to ensure nothing breaks.

1. SSH to the server
([see how](../../../../docs/cardano/integration.md#managing-the-services)).

2. Disable the cardano process:
```
sudo systemctl stop cardano
```

3. Change directory to the one with docker compose definitions:
```
cd /cardano
```

4. Modify the compose file to new versions:
```
sudo vim docker-compose.yml
```

5. Launch docker-compose and ~pray~ have positive thoughts:
```
sudo docker-compose up
```

  * If `db-sync` fails to migrate DB, it's generally best to drop everything and let it rebuild its state:
    ```
    $ psql --host=credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com --user=prism-test-cardano
    prism-test-cardano=> drop owned by "prism-test-cardano";
    ```
  * If new Docker images cannot be downloaded because the disk is full, remove
  old images with:
    ```
    sudo docker image rm <IMAGE>:<TAG>
    ```

6. When everything works update local `docker-compose.yml.tmpl` accordingly

7. Apply the changes (that is going to destroy old instance and deploy a new one, leaving the data intact):
```
./cardano.sh -a
```
