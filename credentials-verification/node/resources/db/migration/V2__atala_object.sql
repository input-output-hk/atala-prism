CREATE DOMAIN ATALA_OBJECT_ID AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE DOMAIN BLOCK_HASH_TYPE AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE atala_objects(
  atala_object_id ATALA_OBJECT_ID NOT NULL,
  sequence_number INTEGER NOT NULL,
  object_timestamp TIMESTAMPTZ NOT NULL,
  atala_block_hash BLOCK_HASH_TYPE NULL DEFAULT NULL,
  object_content BYTEA NULL DEFAULT NULL,
  processed BOOLEAN NOT NULL DEFAULT FALSE,
  -- constraints
  CONSTRAINT atala_objects_pk PRIMARY KEY (atala_object_id),
  CONSTRAINT atala_objects_sequence_number_unique UNIQUE (sequence_number),
  CONSTRAINT atala_objects_sequence_number_positive CHECK (sequence_number > 0)
);

CREATE INDEX atala_objects_sequence_number_index on atala_objects USING BTREE(sequence_number);
