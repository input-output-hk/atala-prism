# Building & Running Cardano

**WARNING:** If you don't care about building everything, and you most likely
don't, follow the [Docker instructions](run-cardano.md) instead.

The following instructions connect to the Cardano Byron Legacy Testnet.

**NOTE:** The instructions in this document were tested on 2020-04-22, and
assume they will be run on Linux, but MacOS is almost identical.

## Setup

### Install dependencies

Ensure you have installed the proper versions of the following tools. Use the
latest, if not specified.

#### Nix

Follow the [Getting Nix](https://nixos.org/nix/download.html) guide, or run:
```shell script
curl -L https://nixos.org/nix/install | sh
```
and source `~/.nix-profile/etc/profile.d/nix.sh` from your `.bashrc` (or
similar) file, so you can easily use commands like `nix-build`.

#### IOHK Cachix

Install Cachix and use the IOHK cache ([haskell.nix](https://github.com/input-output-hk/haskell.nix)):

```shell script
nix-env -iA cachix -f https://cachix.org/api/v1/install
cachix use iohk
```

Ensure the IOHK Hydra binary cache is also used:

```shell script
sudo mkdir -p /etc/nix
cat <<EOF | sudo tee /etc/nix/nix.conf
substituters = https://hydra.iohk.io https://cache.nixos.org/
trusted-substituters =
trusted-public-keys = hydra.iohk.io:f/Ea+s+dFdN+3Y/G+FDgSq+a5NEWhJGzdjvKNGv0/EQ= cache.nixos.org-1:6NCHdD59X431o0gWypbMrAURkbJ16ZPMQFGspcDShjY=
EOF
```

#### Cabal

Install [Cabal](https://www.haskell.org/cabal/index.html#install-upgrade) and
then use it to update itself (distributed version may be old):
```shell script
sudo apt-get install cabal-install
cabal update
cabal install Cabal cabal-install
```
The updated Cabal won't be in your `PATH`, so add it to your `.bashrc` (or
similar) file:
```shell script
export PATH=~/.cabal/bin:$PATH
```

Check Cabal version is at least 3.0:
```shell script
cabal --version
```

#### Stack

Follow the
[How to install](https://docs.haskellstack.org/en/stable/README/#how-to-install)
page, or install it with:
```shell script
curl -sSL https://get.haskellstack.org/ | sh
```
or upgrade it with:
```shell script
stack upgrade
```

Add the following to your `PATH` so `stack install` makes installed binaries
available.
```shell script
export PATH=~/.local/bin:$PATH
```

Ensure you have at least version `2.1.3`:
```shell script
stack --version
```

#### GCH (Glasgow Haskell Compiler)

Cardano repos and dependencies are highly tied to the `base` package version
`4.12` and, in order to get it, the proper GCH version has to be installed as
well. From its [Download](https://www.haskell.org/ghc/download.html) page and
the [Base package versions](https://wiki.haskell.org/Base_package#Versions),
GCH version `8.6.5` is needed.

```shell script
stack --resolver ghc-8.6.5 setup
```
Add `~/.stack/programs/x86_64-linux/ghc-8.6.5` (or similar) to `PATH`:
```shell script
export PATH=~/.stack/programs/x86_64-linux/ghc-8.6.5/bin:$PATH
```
Ensure GHC version is `8.6.5`:
```shell script
ghc --version
```

#### Misc

You may also need the following native libraries:
```shell script
sudo apt-get install libsystemd-dev libz-dev libpq-dev libssl-dev zlib1g-dev
```

### Initialize the components

Clone and compile the following repos at the same level so relative references
work properly.

**NOTE:** Compilation may take hours, but if you check out a tagged release, it
should finish faster.

#### cardano-node

```shell script
git clone https://github.com/input-output-hk/cardano-node
cd cardano-node
nix-build -A scripts.testnet.node -o testnet.node.local
```

If you are interested on some specific release, visit their
[releases page](https://github.com/input-output-hk/cardano-node/releases)
to obtain the tag you need to checkout.

#### cardano-wallet

```shell script
git clone https://github.com/input-output-hk/cardano-wallet
cd cardano-wallet
stack install cardano-wallet-byron
```

If you are interested on some specific release, visit their
[releases page](https://github.com/input-output-hk/cardano-wallet/releases)
to obtain the tag you need to checkout.

#### cardano-db-sync

```shell script
git clone https://github.com/input-output-hk/cardano-db-sync
cd cardano-db-sync
nix-build -A cardano-db-sync -o db-sync-node
PGPASSFILE=config/pgpass-testnet scripts/postgresql-setup.sh --createdb
```

You can use the `--recreatedb` for `postgresql-setup.sh` to drop any previously 
created database.

If you are interested on some specific release, visit their
[releases page](https://github.com/input-output-hk/cardano-db-sync/releases)
to obtain the tag you need to checkout.

### Run the components

Running the components from scratch should take 20-30 min for them to sync with
the testnet.

#### Run the Cardano node

```shell script
cd cardano-node
./testnet.node.local
```

#### Run the Cardano Wallet node

```shell script
cd cardano-wallet
cardano-wallet-byron serve \
    --node-socket ../cardano-node/state-node-testnet/node.socket \
    --testnet ../cardano-node/configuration/mainnet-ci/testnet-genesis.json \
    --database testnet-db \
    --port 8091
```

Note the default value for `--port` is `8090`, but it may clash with Docker so,
to avoid the issue, `8091` is used instead. This is important to keep in mind if
you happen to read documentation about `cardano-wallet`.

#### Run the Cardano DB Sync node

Create the database and start the node:
```shell script
cd cardano-db-sync
PGPASSFILE=config/pgpass-testnet db-sync-node/bin/cardano-db-sync \
    --config config/testnet-config.yaml \
    --genesis-file ../cardano-node/configuration/mainnet-ci/testnet-genesis.json \
    --socket-path ../cardano-node/state-node-testnet/node.socket \
    --schema-dir schema/
```

## Operations

In summary, we will:
1. Create two wallets: `Wallet #1` and `Wallet #2`.
2. Get money from the faucet into `Wallet #1`.
3. Send money from `Wallet #1` to `Wallet #2`.
4. Check the balances of `Wallet #1` and `Wallet #2`.
5. Inspect the payment operation in the PostgreSQL DB.

Note all amounts will be in lovelaces, which is 0.000001 ada.

Also note every transaction takes a few minutes to be verified by the
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

MNEMONIC1=`cardano-wallet-byron mnemonic generate`
echo "$MNEMONIC1
$PASSPHRASE
$PASSPHRASE
" | cardano-wallet-byron wallet create from-mnemonic --port 8091 "Wallet #1"
WALLET1=`cardano-wallet-byron wallet list --port 8091 | jq '.[] | select(.name == "Wallet #1") | .id' -r`

MNEMONIC2=`cardano-wallet-byron mnemonic generate`
echo "$MNEMONIC2
$PASSPHRASE
$PASSPHRASE
" | cardano-wallet-byron wallet create from-mnemonic --port 8091 "Wallet #2"
WALLET2=`cardano-wallet-byron wallet list --port 8091 | jq '.[] | select(.name == "Wallet #2") | .id' -r`
```

Note the wallets created are, by default, Icarus style, and later
transactions will simply operate on the first wallet address, out of the 21
automatically generated ones.

### Fund a wallet

```shell script
ADDRESS1=`cardano-wallet-byron address list --port 8091 $WALLET1 | jq -r ".[0].id"`
curl -s -XPOST https://faucet2.cardano-testnet.iohkdev.io/send-money/$ADDRESS1
```

Note that this is a new faucet, and it only allows one request every 24 hours.
If your request fails because of the rate limit, you can ask a teammate to
transfer you some funds, so you can test.

### Send money

```shell script
TO_SEND_AMOUNT=1000000
ADDRESS2=`cardano-wallet-byron address list --port 8091 $WALLET2 | jq -r ".[0].id"`
echo "$PASSPHRASE
" | cardano-wallet-byron transaction create --port 8091 $WALLET1 --payment $TO_SEND_AMOUNT@$ADDRESS2
```

If you are interested on creating the Cardano transaction manually, please read
[these instructions](https://github.com/input-output-hk/cardano-transactions/wiki/How-to-submit-transaction-via-cardano-tx-CLI).

### Check wallet balances

```shell script
cardano-wallet-byron wallet list --port 8091 | \
    jq ".[] | {name: .name, balance: .balance}"
```

### Inspect the payment in PostgreSQL

Get the transaction ID of the payment (assuming it was the last one):
```shell script
TXID=`cardano-wallet-byron transaction list --port 8091 $WALLET2 | jq ".[0].id" -r`
```

If you would like to see the transaction in Cardano Explorer, run:
```shell script
xdg-open https://explorer.cardano-testnet.iohkdev.io/en/transaction/?id=${TXID}
```

Log into the PostgreSQL DB maintained by the Cardano DB Sync node:
```shell script
PGPASSFILE=config/pgpass_testnet psql cexplorer_testnet --variable=TXID="\x$TXID"
```

Note we are passing in the `TXID` variable with a `\x` prefix, which is how the
database expects it. This variable will be used next, but it isn't needed for
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

You should see the same data you see in Cardano Explorer.
