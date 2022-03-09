# Cardano Metadata Schema

Since Shelley, Cardano provides a way to attach metadata information to a
transaction in the form of a [CBOR](https://cbor.io/) object, but using
[cardano-wallet](https://github.com/input-output-hk/cardano-wallet) and
[cardano-db-sync](https://github.com/input-output-hk/cardano-db-sync) in Atala
PRISM means JSON should be used instead, with some restrictions:
                                      
  - All top-level keys must be integers between 0 and 2<sup>64</sup>-1.
  - Strings must be at most 64 characters.
  - Bytestrings may be encoded as hex-encoded strings prefix with `0x`.

Based on those restrictions, Atala PRISM uses the following schema to store
objects in a Cardano transaction metadata:
```json
{
  <PRISM_INDEX>: {
    "version": <VERSION>,
    "content": [<CONTENT>]
  }
}
```
where:
  - `PRISM_INDEX`: Identifier of an Atala PRISM object. This value is hardcoded
  to `21325`, which is the last 16 bits of `344977920845`, which is the decimal
  representation of the concatenation of the hexadecimal values
  (`50 52 49 53 4d`) of the word `PRISM` in ASCII.
  - `VERSION`: integer version of the schema. Starts at `1` and changes every
  time the schema evolves.
  - `CONTENT`: The array of bytes to store. There are no limits on the size of
  arrays, so it gets restricted only by the maximum transaction size. Such bytes
  represent an `AtalaObject` (see
  [proto](https://github.com/input-output-hk/atala/blob/91f36aa93986fb2df23618b62b9281a55a3ea3c0/prism-sdk/protos/node_internal.proto#L19).

**NOTE**: The maximum size of the metadata is based on the size of the transaction
and the maximum size of any transaction, which is an updatable protocol param
(currently 16 kb, see
[maxTxSize](https://github.com/input-output-hk/cardano-node/blob/master/configuration/cardano/mainnet-shelley-genesis.json#L13)).

## Alternative Options

Other schema options that were contemplated but ended up not being used:

  - `version` without a field name, like `PRISM_INDEX`: it takes less space but
  the difference is so minimal, readability is preferred.
  - A `vendor` field: there are great chances that some deployments want to
  only sync their data, or want to fork Cardano and ignore any previous objects,
  but for now not having it is just easier and could just be embedded in
  `content` instead.
  - A `type` field: this information should be embedded in `content` already.
  - `content` as a bytestring instead of an array of bytes: there is a 64 byte
  limit on bytestrings, which restricts the field unnecessarily.
  - Simplifying all field names to one letter (e.g., use `v` instead of
  `version`): this becomes problematic with clashes, like `version` and `vendor`.
  - Not having string keys, only integer keys: this is just premature
  optimization, having something readable by humans is preferred.

A fee analysis is [here](metadata-schema-fee.md), where schema variations are
compared.
