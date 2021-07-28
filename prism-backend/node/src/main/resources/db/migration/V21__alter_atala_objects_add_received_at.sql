ALTER TABLE atala_objects
    ADD COLUMN received_at TIMESTAMPTZ NULL;

-- Set a default value for objects without timestamp
UPDATE atala_objects
    SET received_at = NOW();

ALTER TABLE atala_objects
    -- Set received_at column mandatory
    ALTER COLUMN received_at SET NOT NULL;

CREATE INDEX atala_objects_received_at on atala_objects USING BTREE(received_at);
