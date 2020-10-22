# Cardano Metadata Schema Fee Analysis

This document explores several JSON schema variations to store Atala PRISM
metadata in Cardano, to optimize transaction fees.

In order to compare the proposed schemas:
1. The [Estimate Fee](https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee)
method of the Cardano Wallet API is used to estimate the fees of an intended
transaction.
2. A [Python script](scripts/fee_estimation.py) creates random payloads and
estimates all proposed schemas using the API above.
3. A real `CreateDID` operation, of 34 bytes, is used for comparing.
4. Random payloads of different sizes are used in order to assess the impact of
payload sizes on the proposed schemas.
5. Given that the Cardano encoding is sensitive to the values in a payload and
not just its size, every payload size is estimated 10 times and the fees
reported are the average of those.
6. All schemas use the same `PRISM_INDEX`, which is a magic number used to
identify an Atala PRISM object.
7. The Cardano Wallet configuration used relies on the same variables used by
Atala PRISM, which can be obtained [here](use-cardano.md).
8. All fee estimation reports a maximum fee of 1 `ada`, so only the minimum
estimated fee is compared. This is due to Atala PRISM making small 1 `ada`
payments and Cardano being unable to give change less than 1 `ada`, representing
a worst case that can be ignored for the estimation analysis.
9. Given Schema 1 is the current schema being used, and expected to be the
most expensive option, it's used as a base for comparisons.

## Schemas

### Schema 1

Schema 1 is the [original design](metadata-schema.md), currently in use, that is
simple and human readable.
```json
{
  <PRISM_INDEX>: {
    "version": 1,
    "content": <CONTENT>
  }
}
```
where `<CONTENT>` is an array of integers, and each element represents a byte
of the serialization of an `AtalaObject`.

### Schema 2

Schema 2 is just like [Schema 1](#schema-1), but with shortened key names.
```json
{
  <PRISM_INDEX>: {
    "v": 2,
    "c": [<CONTENT>]
  }
}
```

### Schema 3

Schema 3 mainly takes advantage of representing bytes as bytestrings, splitting
the original byte array into chunks of no more than 64 bytes. This 64-bytes
limit is imposed by Cardano, and the reason why the original bytes cannot be
represented as one single bytestring.
```json
{
  <PRISM_INDEX>: {
    "v": 3,
    "c": [<CONTENT_CHUNK_BYTESTRING_0>,
          <CONTENT_CHUNK_BYTESTRING_1>,
          ...,
          <CONTENT_CHUNK_BYTESTRING_K>]
  }
}
```
where `K = ceil(size(CONTENT) / 64)` is the number of payload chunks.

## Analysis

Given Schema 2 simply differs from Schema 1 by its keys, it only saves a fixed
amount of 528 `lovelaces`, regardless of payload size.

Schema 3 is the optimal schema and saves more with bigger payloads, because of
base fees charged by Cardano:
  - Publishing a single `CreateDID` operation of 34 bytes is 1804 `lovelaces`
    cheaper, a 1% improvement.
  - Publishing a random payload of 500 bytes (worst case in
    [protocol 0.3](../protocol/protocol-v0.3.md)) is 18009 `lovelaces` cheaper,
    a 9% improvement.

Payloads of 1000 and 8384 bytes were also compared, but should be noted it was
for informational purposes only as Atala PRISM metadata is not expected to be
larger than 500 bytes in most situations.

As a conclusion, Schema 3 is cheaper by a relatively small margin but, given
the engineering cost is low, it could be implemented to replace Schema 1.

### Script output

The following is the actual output obtained by the
[estimation script](scripts/fee_estimation.py) (all amounts in `lovelaces`):
```
Fee of a real payload of 34 bytes:
  Schema 1: 173641 (base)
  Schema 2: 173113 (diff: 528, diff perc: 0%)
  Schema 3: 171837 (diff: 1804, diff perc: 1%)

Average fee of 10 random payloads of 100 bytes each:
  Schema 1: 178855 (base)
  Schema 2: 178327 (diff: 528, diff perc: 0%)
  Schema 3: 174829 (diff: 4026, diff perc: 2%)

Average fee of 10 random payloads of 500 bytes each:
  Schema 1: 210966 (base)
  Schema 2: 210438 (diff: 528, diff perc: 0%)
  Schema 3: 192957 (diff: 18009, diff perc: 9%)

Average fee of 10 random payloads of 1000 bytes each:
  Schema 1: 250751 (base)
  Schema 2: 250223 (diff: 528, diff perc: 0%)
  Schema 3: 215661 (diff: 35090, diff perc: 14%)

Average fee of 10 random payloads of 8384 bytes each:
  Schema 1: 840263 (base)
  Schema 2: 839735 (diff: 528, diff perc: 0%)
  Schema 3: 550721 (diff: 289542, diff perc: 34%)
```
