## To build fat jar
`./gradlew fatJar`

You can a built jar in `app/build/libs` folder.

## To run a CLI app
Move to `app/build/libs` and exec
`java -jar app.jar -h`

As result, you will see something like that
```
Usage: node-cli options_list
Subcommands:
    create-did - Created DID
    health-check - Node health check
    build-info - Get Node build info
    resolve-did - Resolve DID document
```

## CLI commands and options

### Environment
To specify an environment host and port pass `-ho` and `-p`, like:

`java -jar app.jar -ho ppp.atalaprism.io build-info` will print PPP build version:

```
GetNodeBuildInfoResponse(version=1.2-92429cb5, scalaVersion=2.13.6, sbtVersion=1.5.5, unknownFields={})
```

By default (if `-ho` omitted) CLI will request `master.atalaprism.io:50053`.

The following requests to the node are supported.

### Health Check:

`java -jar app.jar health-check`
```Node is up!```

### Get build info:

`java -jar app.jar build-info`
```
GetNodeBuildInfoResponse(version=1.2-1ca39231, scalaVersion=2.13.7, sbtVersion=1.5.5, unknownFields={})
```

### Resolve DID:

`java -jar app.jar resolve-did --did did:prism:3557f05be968a630042026ecb6bedad1ebc2b532c63f938883663e08753a4f71`

```
- Prism DID: did:prism:3557f05be968a630042026ecb6bedad1ebc2b532c63f938883663e08753a4f71
- DID's public keys: PrismKeyInformation(keyId=master0, keyTypeEnum=0, publicKey=io.iohk.atala.prism.crypto.keys.ECPublicKey@487db668, addedOn=LedgerData(transactionId=0076def783a2d20d53b4324e6b2bac1b8171011f67f379f863d6994eb713fde9, ledger=4, timestampInfo=TimestampInfo(atalaBlockTimestamp=1639666295000, atalaBlockSequenceNumber=11, operationSequenceNumber=0)), revokedOn=null)
```

### Create DID

`java -jar app.jar -ho ppp.atalaprism.io create-did`

```
Generates and registers a DID
[main] INFO org.bitcoinj.crypto.MnemonicCode - PBKDF2 took 78.02 ms
- Sent a request to create a new DID to PRISM Node.
- The transaction can take up to 10 minutes to be confirmed by the Cardano network.
- Operation identifier: ef7fb7d71d776ea9508469316bc4da68ccc678b106e295e3319ad5a0793ce9ee

Current operation status: PENDING_SUBMISSION
Current operation status: AWAIT_CONFIRMATION
Transaction id: 685d2495af337773831fb221da98dcaed6d0f1440d8b23093581a2f0d86bd837
Track the transaction in:
- https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=685d2495af337773831fb221da98dcaed6d0f1440d8b23093581a2f0d86bd837
Current operation status: AWAIT_CONFIRMATION
Current operation status: AWAIT_CONFIRMATION
- DID with id did:prism:d2e9001d947cb702c4e2f33ab31786550ba46167972038f94b87051fd7fe5085 is created
- Operation hash: Sha256Digest(value=[-46, -23, 0, 29, -108, 124, -73, 2, -60, -30, -13, 58, -77, 23, -122, 85, 11, -92, 97, 103, -105, 32, 56, -7, 75, -121, 5, 31, -41, -2, 80, -123])

```
