CREATE TYPE PARTICIPANT_TYPE AS ENUM('holder', 'issuer', 'verifier');

-- https://w3c-ccg.github.io/did-spec/#generic-did-syntax
CREATE DOMAIN DID AS TEXT CHECK(
  VALUE ~ '^did:[a-z0-9]+:[a-zA-Z0-9._-]*(:[a-zA-Z0-9._-]*)*$'
);

CREATE TABLE participants(
  id UUID NOT NULL,
  tpe PARTICIPANT_TYPE NOT NULL,
  name text NOT NULL,
  did DID NULL,
  CONSTRAINT participants_id_pk PRIMARY KEY (id)
);

CREATE TABLE connection_tokens(
  token text NOT NULL,
  initiator UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  used_at TIMESTAMPTZ NULL DEFAULT NULL,
  CONSTRAINT connection_tokens_token_pk PRIMARY KEY (token),
  CONSTRAINT connection_tokens_initiator_fk FOREIGN KEY (initiator) REFERENCES participants (id)
);

CREATE INDEX connection_tokens_initiator_index ON connection_tokens USING BTREE (initiator);

CREATE TABLE connections(
  id UUID NOT NULL,
  initiator UUID NOT NULL,
  acceptor UUID NOT NULL,
  instantiated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT connections_id_pk PRIMARY KEY (id),
  CONSTRAINT connections_initiator_fk FOREIGN KEY (initiator) REFERENCES participants(id),
  CONSTRAINT connections_acceptor_fk FOREIGN KEY (acceptor) REFERENCES participants(id),
  CONSTRAINT connections_initiator_acceptor_not_equal CHECK (initiator != acceptor)
);

CREATE INDEX connections_initiator_index ON connections USING BTREE (initiator);
CREATE INDEX connections_acceptor_index ON connections USING BTREE (acceptor);
CREATE INDEX connections_instantiated_at_index ON connections USING BTREE (instantiated_at);

INSERT INTO participants (id, tpe, name, did) VALUES ('c8834532-eade-11e9-a88d-d8f2ca059830', 'issuer', 'Issuer 1', 'did:test:issuer-1');
INSERT INTO participants (id, tpe, name, did) VALUES ('e20a974e-eade-11e9-a447-d8f2ca059830', 'holder', 'Holder 1', 'did:test:holder-1');
