
CREATE DOMAIN BITCOIN_TXID_TYPE AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE DOMAIN ATALA_OBJECT_ID AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);


CREATE TABLE atala_objects(
  atala_object_id ATALA_OBJECT_ID NOT NULL,
  bitcoin_txid BITCOIN_TXID_TYPE NOT NULL,
  -- constraints
  CONSTRAINT atala_objects_pk PRIMARY KEY (atala_object_id),
  CONSTRAINT atala_objects_bitcoin_txid_unique UNIQUE (bitcoin_txid)
)

