-- ATA-4314
-- In ATA-3100 review, we realised that we were leaking implementation details
-- related to merkle inclusion proofs.
-- For this reason, we implemented an encoding for these proofs and now we need
-- to update tables accordingly.

-- The table is still empty, but for sake of sanity, we start by cleaning it up
DELETE FROM published_credentials;

ALTER TABLE published_credentials
    DROP COLUMN inclusion_proof_hash,
    DROP COLUMN inclusion_proof_index,
    DROP COLUMN inclusion_proof_siblings;

-- We now add the single column to store the encoded proof
-- We opted for the TEXT type
-- This seems to be good for our proofs, note that a proof encodes
-- + 1 hash (32 bytes encoded as hex)
-- + 1 32 bits integer (currently encoded as text)
-- + N hex encoded hashes, where N is the height of the tree
ALTER TABLE published_credentials
    ADD COLUMN inclusion_proof TEXT NOT NULL;
