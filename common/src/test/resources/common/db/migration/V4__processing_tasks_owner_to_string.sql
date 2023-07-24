ALTER TABLE processing_tasks DROP COLUMN owner;
ALTER TABLE processing_tasks ADD COLUMN owner TEXT NULL;
