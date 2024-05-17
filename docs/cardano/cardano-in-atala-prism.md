# Cardano in Atala Prism

## Objective

Outline how Atala Prism can use Cardano as the blockchain backing its
operations.

## Background

In order for Atala Prism to work, it needs to be able to store and query
metadata in a blockchain. Atala Prism does through cardano blockchain.

### Cardano-related code

Below are some important Cardano-related code repositories and a summary of
their goals and how they are, or are not, important to Atala Prism.

* [*cardano-node*](https://github.com/IntersectMBO/cardano-node)

  The core component to participate in a Cardano blockchain.

  Docker images can be found as
  [*intersectmbo/cardano-node*](https://github.com/IntersectMBO/cardano-node/pkgs/container/cardano-node).

* [*cardano-explorer*](https://github.com/input-output-hk/cardano-explorer)

  Archived code that was split into *cardano-db-sync*, *cardano-rest*, and
  *cardano-graphql*, detailed below.

* [*cardano-transactions*](https://github.com/IntersectMBO/cardano-transactions)

  CLI and Haskell library for building Cardano transactions. End-to-end example
  on how to use it is
  [*here*](https://github.com/IntersectMBO/cardano-transactions/wiki/How-to-submit-transaction-via-cardano-tx-CLI).
  This library is used by other Cardano projects.

  No docker images exist for this as it's used as a library.

* [*cardano-wallet*](https://github.com/cardano-foundation/cardano-wallet)

  [*HTTP API*](https://cardano-foundation.github.io/cardano-wallet/api/edge/) to
  manage Ada in a high-level fashion without UTxOs, with a handy CLI translating
  commands into API calls. It connects to a locally running *cardano-node*, and
  can send payments hiding the complexity of building transactions manually. The
  Adrestia team has said that both API and CLI will support attaching metadata
  to payments (it should imply *cardano-transactions* will do as well).

  Docker images can be found as
  [*inputoutput/cardano-wallet*](https://hub.docker.com/r/cardanofoundation/cardano-wallet/tags).

* [*cardano-db-sync*](https://github.com/IntersectMBO/cardano-db-sync)

  A component that follows a locally running Cardano node and stores blocks and
  transactions in PostgreSQL. The DB is meant to be used as read-only from other
  applications.

  Docker images can be found as
  [*intersectmbo/cardano-db-sync*](https://github.com/IntersectMBO/cardano-db-sync/pkgs/container/cardano-db-sync).

* [*explorer-api*](https://input-output-hk.github.io/cardano-rest/explorer-api/)
  
  explorer-api allows to query blockchain data 
  
* [*submit-api*](https://input-output-hk.github.io/cardano-rest/submit-api/)

  submit-api allows to submit a new transaction into the blockchain
  #### (explorer/submit)
  
  They won't get any new features, meaning *explorer-api* won't return metadata but, given
  *submit-api* only takes a serialized signed transaction as argument, one can
  expect to construct a transaction with metadata and be able to submit it via
  *submit-api* (transaction construction is decoupled from submission).

  Docker image:
  [*inputoutput/cardano-submit-api*](https://github.com/IntersectMBO/cardano-node/pkgs/container/cardano-submit-api)


* [*cardano-graphql*](https://github.com/cardano-foundation/cardano-graphql)

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

## Integration

Atala Prism integrates with:

* cardano-node: It's the actual blockchain, so the most important piece.

* cardano-db-sync: Integrating with *cardano-graphql* is more cumbersome and in
  order to keep it simple, Atala Prism will query the underlying PostgreSQL DB
  to find out the state of the blockchain.

* cardano-wallet: Atala Prism does not need to manage UTxOs at the moment, and
  the [*Cardano Wallet Backend
  API*](https://cardano-foundation.github.io/cardano-wallet/api/edge/) simplifies
  submitting transactions to Cardano.
.
