-- First, we need to remove rows with duplicate DID's
-- the cause of an error in recovering a wallet if a user added the same did twice
DELETE FROM
    participants a
        USING participants b
WHERE
    a.id < b.id
    AND a.did = b.did;
-- New unique DID column constraint to guard against such situations (adding of the same DID twice)
ALTER TABLE participants
    ADD CONSTRAINT participants_did_unique UNIQUE (did);