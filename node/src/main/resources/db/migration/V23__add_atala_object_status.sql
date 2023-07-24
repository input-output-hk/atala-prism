CREATE TYPE ATALA_OBJECT_STATUS AS ENUM('PENDING', 'MERGED', 'PROCESSED');

ALTER TABLE atala_objects
  ADD COLUMN atala_object_status ATALA_OBJECT_STATUS DEFAULT 'PENDING';

UPDATE atala_objects
  SET atala_object_status = 'PROCESSED'
  WHERE processed = true;

ALTER TABLE atala_objects DROP COLUMN processed;
