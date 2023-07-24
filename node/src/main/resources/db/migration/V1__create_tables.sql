
CREATE DOMAIN BLOCKHASH_TYPE AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE DOMAIN NON_NEGATIVE_INT_TYPE AS INT
CHECK (
  VALUE >= 0
);

CREATE TABLE blocks(
  blockhash BLOCKHASH_TYPE NOT NULL,
  previous_blockhash BLOCKHASH_TYPE NULL,
  height NON_NEGATIVE_INT_TYPE NOT NULL,
  time BIGINT NOT NULL,
  -- constraints
  CONSTRAINT blocks_blockhash_pk PRIMARY KEY (blockhash),
  CONSTRAINT blocks_height_unique UNIQUE (height),
  CONSTRAINT blocks_previous_blockhash_fk FOREIGN KEY (previous_blockhash) REFERENCES blocks (blockhash)
);

CREATE INDEX blocks_time_index ON blocks USING BTREE (time);
CREATE INDEX blocks_previous_blockhash_index ON blocks USING BTREE (previous_blockhash);
