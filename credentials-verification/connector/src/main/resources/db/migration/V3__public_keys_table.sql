
CREATE TABLE holder_public_keys(
  participant_id UUID NOT NULL, -- a holder must have exactly one key
  x NUMERIC(1000) NOT NULL,
  y NUMERIC(1000) NOT NULL,
  CONSTRAINT holder_public_keys_participant_id_pk PRIMARY KEY (participant_id),
  CONSTRAINT holder_public_keys_participant_id_fk FOREIGN KEY (participant_id) REFERENCES participants (id)
);
