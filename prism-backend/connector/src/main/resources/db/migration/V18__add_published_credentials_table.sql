CREATE TABLE published_credentials (
  credential_id UUID NOT NULL,
    --^ the id that the cmanager assigns to the credential
  node_credential_id TEXT NOT NULL,
    --^ the id that the node assigns to the credential
  operation_hash TEXT NOT NULL,
    --^ the hex encoded hash of the AtalaOperation that is used to issue the credential
  encoded_signed_credential TEXT NOT NULL,
    --^ the encoded signed credential (e.g. a compact JWS string)
  CONSTRAINT published_credentials_pk PRIMARY KEY (credential_id),
  CONSTRAINT published_credentials_credential_id_fk FOREIGN KEY (credential_id) REFERENCES credentials(credential_id)
);