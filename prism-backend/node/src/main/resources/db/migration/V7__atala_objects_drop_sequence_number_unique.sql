-- atala_objects.sequence_number is not constrained anymore
ALTER TABLE atala_objects
    DROP CONSTRAINT atala_objects_sequence_number_unique,
    DROP CONSTRAINT atala_objects_sequence_number_positive;

-- It also does not need to be queried directly anymore
DROP INDEX atala_objects_sequence_number_index;
