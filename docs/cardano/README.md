# Cardano

In order for Atala Prism to work, it needs to be able to store and query
metadata in a blockchain.

## Integration

Atala Prism integrates with:
* [cardano-node](https://github.com/IntersectMBO/cardano-node):
  The actual blockchain.
* [cardano-db-sync](https://github.com/IntersectMBO/cardano-db-sync):
  Atala Prism queries the underlying PostgreSQL DB to find out the state of
  the blockchain.
* [cardano-wallet](https://github.com/cardano-foundation/cardano-wallet):
  Atala Prism does not need to manage UTxOs at the moment, and the
  [Cardano Wallet Backend API](https://input-output-hk.github.io/cardano-wallet/api/edge/)
  simplifies submitting transactions to Cardano.

The schema used to store Atala PRISM objects in Cardano is
[here](metadata-schema.md).

For more context around the plan used for integration, please read the
[Cardano in Atala Prism](cardano-in-atala-prism.md)
document.

