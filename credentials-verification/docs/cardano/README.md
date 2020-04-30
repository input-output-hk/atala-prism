# Cardano

In order for Atala Prism to work, it needs to be able to store and query
metadata in a blockchain. Cardano currently does not offer a way to query or
store metadata, but it will when it enters the Shelley era.

## Integration plan

Atala Prism will integrate with:
* [cardano-node](https://github.com/input-output-hk/cardano-node):
  The actual blockchain.
* [cardano-db-sync](https://github.com/input-output-hk/cardano-db-sync):
  Atala Prism will query the underlying PostgreSQL DB to find out the state of
  the blockchain.
* [cardano-wallet](https://github.com/input-output-hk/cardano-wallet):
  Atala Prism does not need to manage UTxOs at the moment, and the
  [Cardano Wallet Backend API](https://input-output-hk.github.io/cardano-wallet/api/edge/)
  simplifies submitting transactions to Cardano.

Once metadata is available, the changes to support it will be implemented,
without requiring new components.

For more context around this plan, please read the
[Cardano in Atala Prism](cardano-in-atala-prism.md)
document.

## Running Cardano

The steps to run the Cardano components to integrate with, are detailed
[here](run-cardano.md).
