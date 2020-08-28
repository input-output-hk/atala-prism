# Encrypted Data Vault

The **Atala PRISM Encrypted Data Vault (EDV)** is a service providing long-term
storage for encrypted user data that can only be decrypted by the user itself,
guaranteeing privacy.

The EDV has been designed to allow the Connector service to drop long-term
storage while still allowing the mobile wallets to recover their data.

## Design

The EDV is provided as a GRPC service with the following operations:

-  *Store*: Stores a given encrypted payload, associating it with the
authenticated user. A unique ID is returned.
-  *Get*: Retrieves all encrypted payloads stored by the authenticated
user, paginated and sorted by insertion order.

### Privacy

The EDV uses the same authorization infrastructure as the Connector, so
communication is secure and the user is authenticated via its DID.

On the client side, the payloads are encrypted with the user DID, so bad
actors cannot steal the information from either the wire or the server end.
Because the server never sees raw data, there are no privacy concerns.

The client can only query its own data, as the server filters payloads
implicitly by the authenticated user.

When a user queries for their payloads, only encrypted data will be returned
and the client will need to decrypt them locally to use them.

### Storage

A PostgreSQL DB stores the encrypted data, along with the DID used to encrypt
it. This way the client can query its own data, and only that.

The current schema is:
```sql
CREATE DOMAIN DID AS TEXT CHECK(
  VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE TABLE payloads (
  id UUID PRIMARY KEY,
  did DID NOT NULL,
  content BYTEA NOT NULL,
  creation_order BIGSERIAL,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX did_index ON payloads USING BTREE (did);
```

### Caveats

Some caveats apply to the current design:

-  The term *user* is used generically to name who is calling the service. Many
*users* could be represented by the same persona, as the service does no try to
identify them.
-  Currently, deleting or updating payloads is not possible as the current use
cases do not need such features. This may change later with new upcoming
requirements, for which an extra metadata field *isModifiable*, specified when
storing a new payload, can be added to denote a payload that can be either
deleted or updated. This new field will be needed to support the existing
requirement of avoiding accidental modification or deletion of important data.
-  No fine-grained query capabilities are offered, as current use cases do not
demand it (i.e., users restoring data need all the data at once).
-  User quota is scheduled to be implemented, but not yet. This is required in
order to control how much data every user can store, and to potentially offer
higher capacity at a premium.
-  The storage solution needs to index the encrypted data, and a
searchable symmetric encryption scheme is required to strengthen user privacy.
Given that the service validates the user talking to it is the proper one and
owns the right DID, the concern of not having a searchable encryption scheme is
not that big. The EDV will eventually need to enforce user quota, which may be a
conflicting requirement with the searchable encryption scheme.
-  Adding the EDV to the Connector server was the easiest way to start.
Eventually, we may want to promote it to its own server and have its own SLA.
The EDV database is separate from the Connector's though.
-  In its first version, the EDV won't implement the drafted W3C standard for
[Encrypted Data Vaults](https://digitalbazaar.github.io/encrypted-data-vaults),
but it's an important goal we will have in mind to eventually fulfill. Doing so
would open up doors and could lead to integrate with the Microsoft Identity Hub
and increase revenue.
