-- Set a default value for objects without content
-- (this may happen only in a local dev env, so it's fine to corrupt it)
UPDATE atala_objects
    SET object_content = ''
    WHERE object_content IS NULL;

ALTER TABLE atala_objects
    -- Delete atala_block_hash as it's no longer used (in favor of object_content)
    DROP COLUMN atala_block_hash,
    -- Make object_content required
    ALTER COLUMN object_content SET NOT NULL;
