CREATE TABLE trusted_proposers(
  did_suffix id_type,

  CONSTRAINT trusted_proposers_pk PRIMARY KEY (did_suffix)
);

ALTER TABLE protocol_versions
  ADD CONSTRAINT proposer_fk
  FOREIGN KEY (proposer_did)
  REFERENCES trusted_proposers (did_suffix);
