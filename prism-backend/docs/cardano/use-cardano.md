# Using Cardano

There are two options to interact with Cardano locally:
  - Run all Cardano components locally and connect to them
  ([how](#connect-to-local-cardano)). This is recommended when making changes
  to the Cardano integration, as messing up does not affect anyone else.
  - Re-use the remote Cardano components used by CircleCI and develop/test
  instances, and connect to them ([how](#connect-to-remote-cardano)).
  This is recommended when simply wanting to use Cardano.

## Connect to Local Cardano

In order to locally run all Cardano components, follow these
[instructions](run-cardano.md).

Once all Cardano components are up and running, configure the following
variables in order to connect to them:
```shell script
export GEUD_NODE_CARDANO_WALLET_PASSPHRASE=$PASSPHRASE
export GEUD_NODE_CARDANO_WALLET_ID=$WALLET1
export GEUD_NODE_CARDANO_PAYMENT_ADDRESS=`cardano-wallet address list $GEUD_NODE_CARDANO_WALLET_ID | jq -r ".[0].id"`
```
Where:
  - `$PASSPHRASE` is whatever passphrase you used for your wallet, most likely
  `mypassphrase`.
  - `$WALLET1` is the ID of your wallet with funds.

Now you can run the Node connecting to local Cardano:
```shell script
GEUD_NODE_LEDGER="cardano" sbt node/run
```

## Connect to Remote Cardano

Obtain the remote Cardano settings with:
```shell script
./.circleci/generate_cardano_variables.sh > cardano-variables.sh
```
If that command fails, you most likely have no SSH access and need to request
a teammate to manually add your key. In the meantime, you can ask for this file.

Given the Cardano Wallet has been configured manually, request the
`wallet-variables.sh` file from a teammate.

Once you have both files, source their variables:
```shell script
source cardano-variables.sh
source wallet-variables.sh
```

Now you can run the Node connecting to remote Cardano:
```shell script
GEUD_NODE_LEDGER="cardano" sbt node/run
```
