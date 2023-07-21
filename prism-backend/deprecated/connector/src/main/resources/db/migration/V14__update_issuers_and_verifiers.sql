ALTER TABLE issuers
    DROP CONSTRAINT issuers_did_unique,
    DROP COLUMN did,
    DROP COLUMN name,
    ADD CONSTRAINT issuers_participants_id_fk FOREIGN KEY (issuer_id) REFERENCES participants (id);

ALTER TABLE verifiers
    RENAME COLUMN user_id TO verifier_id;

ALTER TABLE verifiers
    ADD CONSTRAINT verifiers_participants_id_fk FOREIGN KEY (verifier_id) REFERENCES participants (id);

ALTER TABLE verifiers
    RENAME CONSTRAINT store_users_pk TO verifiers_pk;
