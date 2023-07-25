# Cardano in Atala Prism

## Objective

Outline how Atala Prism can use Cardano as the blockchain backing its
operations.

## Background

In order for Atala Prism to work, it needs to be able to store and query
metadata in a blockchain. Atala Prism already does this in Bitcoin, and now
needs to do it in Cardano. Cardano currently does not offer a way to query or
store metadata but it will when it enters the Shelley era
([*spec*](https://github.com/input-output-hk/cardano-ledger-specs/blob/b0f4b024962eb834af55151697c8da72b6df4df8/shelley/chain-and-ledger/executable-spec/cddl-files/shelley.cddl#L162)).
Unfortunately, this will be completed in the order of months, hence it may block
Atala Prism from launching.

### Cardano-related code

Below are some important Cardano-related code repositories and a summary of
their goals and how they are, or are not, important to Atala Prism.

* [*cardano-node*](https://github.com/input-output-hk/cardano-node)

  The core component to participate in a Cardano blockchain.

  Docker images can be found as
  [*inputoutput/cardano-node*](https://hub.docker.com/r/inputoutput/cardano-node/tags).

* [*adrestia*](https://github.com/input-output-hk/adrestia)

  Catalog of projects that fall under the scope of Adrestia: the product team
  working on developing tooling and client interfaces around Cardano.

* [*cardano-explorer*](https://github.com/input-output-hk/cardano-explorer)

  Archived code that was split into *cardano-db-sync*, *cardano-rest*, and
  *cardano-graphql*, detailed below.

* [*cardano-transactions*](https://github.com/input-output-hk/cardano-transactions)

  CLI and Haskell library for building Cardano transactions. End-to-end example
  on how to use it is
  [*here*](https://github.com/input-output-hk/cardano-transactions/wiki/How-to-submit-transaction-via-cardano-tx-CLI).
  This library is used by other Cardano projects.

  No docker images exist for this as it's used as a library.

* [*cardano-wallet*](https://github.com/input-output-hk/cardano-wallet)

  [*HTTP API*](https://input-output-hk.github.io/cardano-wallet/api/edge/) to
  manage Ada in a high-level fashion without UTxOs, with a handy CLI translating
  commands into API calls. It connects to a locally running *cardano-node*, and
  can send payments hiding the complexity of building transactions manually. The
  Adrestia team has said that both API and CLI will support attaching metadata
  to payments (it should imply *cardano-transactions* will do as well).

  Docker images can be found as
  [*inputoutput/cardano-wallet*](https://hub.docker.com/r/inputoutput/cardano-wallet/tags).

* [*cardano-db-sync*](https://github.com/input-output-hk/cardano-db-sync)

  A component that follows a locally running Cardano node and stores blocks and
  transactions in PostgreSQL. The DB is meant to be used as read-only from other
  applications.

  Docker images can be found as
  [*inputoutput/cardano-db-sync*](https://hub.docker.com/r/inputoutput/cardano-db-sync/tags).

* [*cardano-rest*](https://github.com/input-output-hk/cardano-rest)

  Currently supported REST APIs for interacting with the Cardano blockchain.
  They will keep getting support to work with Shelley, but no new features, like
  metadata, will be added. It's meant to be eventually replaced by
  *cardano-graphql* below.

  The REST APIs are split in two:
  [*explorer-api*](https://input-output-hk.github.io/cardano-rest/explorer-api/)
  and
  [*submit-api*](https://input-output-hk.github.io/cardano-rest/submit-api/).
  *explorer-api* allows to query blockchain data, whereas *submit-api* allows to
  submit a new transaction into the blockchain. As stated above, they won't get
  any new features, meaning *explorer-api* won't return metadata but, given
  *submit-api* only takes a serialized signed transaction as argument, one can
  expect to construct a transaction with metadata and be able to submit it via
  *submit-api* (transaction construction is decoupled from submission).

  Docker images can be found as
  [*inputoutput/cardano-submit-api*](https://hub.docker.com/r/inputoutput/cardano-submit-api/tags)
  and
  [*inputoutput/cardano-explorer-api*](https://hub.docker.com/r/inputoutput/cardano-explorer-api/tags).

* [*cardano-graphql*](https://github.com/input-output-hk/cardano-graphql)

  Cross-platform, typed, and queryable API service for Cardano. Currently, it
  only generates TypeScript libraries, but Scala can be added. As of today, it
  only supports queries but the Adrestia team has said it will support
  transaction submission in a few months from now. There's no metadata query
  support either, but it will also be added later.

  Docker images can be found as
  [*inputoutput/cardano-graphql*](https://hub.docker.com/r/inputoutput/cardano-graphql/tags).

* [*jormungandr*](https://github.com/input-output-hk/jormungandr)

  Node implementation, written in rust, with the initial aim to support the
  Ouroboros type of consensus protocol. It's not expected to have metadata
  support and the code itself seems expected to be eventually discarded, so it
  isn't interesting to Atala Prism.

## Integration plan

After discussing with members of the Adrestia team, Atala Prism will
integrate with:

* cardano-node: It's the actual blockchain, so the most important piece.

* cardano-db-sync: Integrating with *cardano-graphql* is more cumbersome and in
  order to keep it simple, Atala Prism will query the underlying PostgreSQL DB
  to find out the state of the blockchain.

* cardano-wallet: Atala Prism does not need to manage UTxOs at the moment, and
  the [*Cardano Wallet Backend
  API*](https://input-output-hk.github.io/cardano-wallet/api/edge/) simplifies
  submitting transactions to Cardano.

All integration can be started immediately without accounting for metadata,
which will be supported eventually by the projects above. By doing so, the
development team can make progress today, incorporating metadata support once
it's ready, as the Wallet API and the DB structure won't change much.

A former suggestion, by the Adrestia team, was to integrate with
*cardano-graphql* and wait for metadata and transaction submission support.
Unfortunately, this does require way more work for Atala Prism, and it's rather
unnecessary given the points made above.


## How to integrate

The components to run are:


|Repo               |      Docker image                                        |
|-------------------|----------------------------------------------------------|
| N/A               | `postgres:${POSTGRES_VERSION}`                           |
| `cardano-node`    | `inputoutput/cardano-node:${CARDANO_NODE_VERSION}`       |
| `cardano-wallet`  | `inputoutput/cardano-wallet:${CARDANO_WALLET_VERSION}`   |
| `cardano-db-sync` | `inputoutput/cardano-db-sync:${CARDANO_DB_SYNC_VERSION}` |

With the following default values:

```sh
    POSTGRES_VERSION=11.5-alpine
    CARDANO_NODE_VERSION=1.10.1
    CARDANO_WALLET_VERSION=dev-master-byron
    CARDANO_DB_SYNC_VERSION=master
```

Both *cardano-node* and *cardano-db-sync* will automatically connect to the
Byron Legacy Testnet when *NETWORK=testnet*. *cardano-wallet* needs to be
instructed where *cardano-node* is running (linux socket), and what
configuration is being used (JSON file). *cardano-db-sync*'s Docker
configuration
([*link*](https://github.com/input-output-hk/cardano-db-sync/blob/master/docker-compose.yml))
can be used as a template for Atala Prism.

About the blockchain to connect to, the following are some caveats:

1. Atala Prism will integrate with the Byron Legacy Testnet for the time being
   to ease up the integration.

2. Given \#1 will slow down blockchain synchronization, integration testing will
   preferably have fixed always-running *cardano-node* and *cardano-db-sync*
   components.

3. Eventually, \#2 can be replaced by on-demand creation of a random testnet
   configuration.
   [*shelley-testnet.sh*](https://github.com/input-output-hk/cardano-node/blob/master/scripts/shelley-testnet.sh)
   can be of help on how to achieve this.
