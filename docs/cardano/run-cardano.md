# Running Cardano

The following instructions connect to the Cardano Shelley Testnet.

## Setup

### Install Docker

Install Docker and `docker-compose` with
[these Linux instructions](https://stackoverflow.com/a/49839172),
and ensure it's at least version `1.23.2`:
```shell script
docker-compose version
```

### Install Cardano Wallet CLI

You can install the latest
[Cardano Wallet CLI](https://github.com/input-output-hk/cardano-wallet/releases)
release for Shelley, but it's suggested to keep it simple with Docker:
```shell script
alias cardano-wallet="docker run --network host --rm -i inputoutput/cardano-wallet:2020.9.11-shelley"
```

However you install it, ensure you got the right version
`2020.9.11 (git revision: 0525347dc0c1e5a0ed1ba35600a93b509655b2e4)`:
```shell script
cardano-wallet version
```

### Clone `cardano-node`

Clone `cardano-node` at the same level of `atala`:
```shell script
git clone https://github.com/input-output-hk/cardano-node
```

We are only interested on file
`cardano-node/configuration/mainnet-ci/testnet-genesis.json`,
which will be referenced relatively from
[docker-compose.yml](docker/docker-compose.yml).

### Configure the shared data

In order for components to not start over after restarts, we bind some Docker
volumes to local directories, ensure the following exist:
```shell script
cd prism-backend/docs/cardano/docker
mkdir -p \
    cardano-data/node-db \
    cardano-data/node-ipc \
    cardano-data/postgres \
    cardano-data/wallet-db \
    cardano-data/db-sync-data
```

## Run the components

Running the components from scratch should take ~1 hour for them to sync with
the testnet:
```shell script
cd prism-backend/docs/cardano/docker
docker-compose up
```

You can check on the sync progress with:
```shell script
alias watch='watch '
watch 'cardano-wallet network information | jq ".sync_progress"'
```
Note the `alias` hack to expand `cardano-wallet` when using Docker. 

## Operations

In summary, we will:
1. Create two wallets: `Wallet #1` and `Wallet #2`.
2. Get money from the faucet into `Wallet #1`.
3. Send money from `Wallet #1` to `Wallet #2`.
4. Check the balances of `Wallet #1` and `Wallet #2`.
5. Inspect the payment operation in the PostgreSQL DB.

Note all amounts will be in lovelaces, which is 0.000001 ada.

Also, note every transaction takes a few minutes to be verified by the
blockchain.

To interact with the wallet, we will use its
[CLI](https://github.com/input-output-hk/cardano-wallet/wiki/Wallet-Command-Line-Interface-(cardano-wallet-byron)),
instead of its
[API](https://input-output-hk.github.io/cardano-wallet/api/edge/),
for simplicity.

### Create the wallets

To create a wallet we need a mnemonic and a passphrase with its confirmation,
hence why you will see `$PASSPHRASE` repeated below.

We will store the wallet IDs into `WALLET1` and `WALLET2`, to perform operations
later, and you can skip these steps as long as you set such variables.

```shell script
PASSPHRASE=mypassphrase

MNEMONIC1=`cardano-wallet recovery-phrase generate`
echo "$MNEMONIC1

$PASSPHRASE
$PASSPHRASE
" | cardano-wallet wallet create from-recovery-phrase "Wallet #1"
WALLET1=`cardano-wallet wallet list | jq '.[] | select(.name == "Wallet #1") | .id' -r`

MNEMONIC2=`cardano-wallet recovery-phrase generate`
echo "$MNEMONIC2

$PASSPHRASE
$PASSPHRASE
" | cardano-wallet wallet create from-recovery-phrase "Wallet #2"
WALLET2=`cardano-wallet wallet list | jq '.[] | select(.name == "Wallet #2") | .id' -r`
```

Note the wallets created are, by default, Icarus style, and later
transactions will simply operate on the first wallet address, out of the 21
automatically generated ones.

### Fund a wallet

Obtain address:

```shell script
cardano-wallet address list $WALLET1 | jq -r ".[0].id"
```


Open https://testnets.cardano.org/en/cardano/tools/faucet/ page, paste the address,
and request funds.

Note that the faucet only allows one request per user every 24 hours.
If your request fails because of the rate limit, you can ask a teammate to
transfer you some funds, so you can test.

### Send money

Verify you have enough funds in `WALLET1` to send (including fees):
```shell script
alias watch='watch '
watch "cardano-wallet wallet get $WALLET1 | jq '.balance'"
```

**NOTE**: Waiting for funds may take some minutes, as the transaction needs to
be verified.

```shell script
TO_SEND_AMOUNT=1000000
ADDRESS2=`cardano-wallet address list $WALLET2 | jq -r ".[0].id"`
echo "$PASSPHRASE
" | cardano-wallet transaction create $WALLET1 --payment $TO_SEND_AMOUNT@$ADDRESS2
```

If you are interested on creating the Cardano transaction manually, please read
[these instructions](https://github.com/input-output-hk/cardano-transactions/wiki/How-to-submit-transaction-via-cardano-tx-CLI).

### Check wallet balances

Check the balances of your wallets, the transaction should take 20 seconds to
be reflected:
```shell script
alias watch='watch '
watch 'cardano-wallet wallet list | jq ".[] | {name: .name, balance: .balance}"'
```

### Inspect the payment in PostgreSQL

Once your wallet balances have been updated, get the transaction ID of the
payment (assuming it was the last one):
```shell script
TXID=`cardano-wallet transaction list $WALLET2 | jq ".[0].id" -r`
```

If you would like to see the transaction in Cardano Explorer, run:
```shell script
xdg-open https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=${TXID}
```

Log into the PostgreSQL DB maintained by the Cardano DB Sync node:
```shell script
cd prism-backend/docs/cardano/docker
PGPASSWORD=$(cat secrets/postgres_password) \
  psql $(cat secrets/postgres_db) \
    -U $(cat secrets/postgres_user) \
    -h localhost \
    -p 5433 \
    --variable=TXID="\x${TXID?}"
```

Note we are passing in the `TXID` variable with a `\x` prefix, so it gets
decoded into bytes. This variable will be used next, but it isn't needed for
logging in.

Run the following query to get the details for the transaction:
```sql
SELECT
    block.time AS time,
    tx.hash AS tx_hash,
    tx.fee AS tx_fee,
    tx.out_sum AS out_value_sum,
    tx_origin.address AS in_address,
    tx_out.address AS out_address,
    tx_out.value AS out_value
  FROM tx
    JOIN block ON block.id = tx.block
    JOIN tx_out ON tx_out.tx_id = tx.id
    JOIN tx_in ON tx_in.tx_in_id = tx.id
    JOIN tx_out AS tx_origin ON tx_origin.tx_id = tx_in.tx_out_id AND tx_origin.index = tx_in.tx_out_index
  WHERE tx.hash = :'TXID'
  ORDER BY tx.id, tx_out.index;
```

Also, run the following to inspect the transaction metadata:
```sql
SELECT tx_metadata.key, tx_metadata.json
  FROM tx
    JOIN tx_metadata ON tx_metadata.tx_id = tx.id
  WHERE tx.hash = :'TXID'
  ORDER BY tx_metadata.key;
```

If you don't get any results, `cardano-db-sync` may still be syncing, run the
following query to find out how old the latest block is:
```sql
SELECT NOW() - MAX(time) AS latest_block_age FROM block;
```

You should see the same data you see in Cardano Explorer, except metadata,
which has not been added yet.
