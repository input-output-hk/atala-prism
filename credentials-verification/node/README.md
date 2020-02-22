## Running Node

Node requires Postgres database to run. You can easily provide it using Docker:
```
docker run -it --rm -e POSTGRES_DB=geud_node_db -p 5432:5432 postgres
```

The default setting is to use bitcoin, which requires separate client. For testing it is much easier to use in memory ledger. It can be done by setting `GEUD_NODE_LEDGER` environment variable:
```
GEUD_NODE_LEDGER="in-memory" mill -i node.run
```

## Node client

Node comes with CLI client, allowing to interact with it. Client has simple state storage capabilities - it saves private keys and ids of last operations for the DID and credentials (as each operation modifying an entity needs the hash of the previous operation affecting it).

**Please note that all operations (like DID creation, credential issuance or revocation) are not applied during request processing - they can stil fail during processing, e.g. because of invalid signature. In case if something doesn't work please check node logs.**

### Create DID

```
mill -i node.client.run create-did --generate-master-key master --generate-issuing-key issuing --generate-issuing-key another_issuing
```

Client sends operation to create a DID and saves private keys and the DID suffix, so following operations don't need to specify the DID.

You can customize GRPC host and port (using `--host` and `--port` options), and the file used to store the state (using `--storage`).

### Issue credential

There are two ways of issuing a credential

1. By providing the content to be hashed
```
mill -i node.client.run issue-credential --content "credential content"
```

2. By providing precomputed hash of the content:
```
mill -i node.client.run issue-credential --content-hash 758b79582e7c61a0cad56bd1c522880e2dc5d2122ab19868b155a62843cc5734
```

Issuing a credential requires having DID and keys to it. If it has been created using the client, it should be retrieved from state storage. Otherwise you need to provide it using `--issuer` arument.

## Revoke credential

In order to revoke the credential you need to run the command with its id:

```
mill -i node.client.run revoke-credential --credential 06029555d620331dedc886eb65832f959eddf359b97a82148845b5a83a81abc2
```

## Update a DID

In order to update the DID use `update-did` command. Use `--generate-[usage]-key <key-id>` to add new keys and `--remove-key` to remove key from DID.

```
mill -i node.client.run update-did --generate-authentication-key authentication --remove-key another_issuing
```

## Resolve a DID

Resolving DID is easy:

```
mill -i node.client.run resolve did:atala:7abbb09e1f14edcfd06a59b3ce6cf82f49e0a185ca7fc4ba6a655bd3817e6185
```

Please note that DID is in the form `did:atala:[did suffix]` where did suffix is the id returned by the creation operation.
