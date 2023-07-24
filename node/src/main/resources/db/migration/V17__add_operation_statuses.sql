CREATE TYPE ATALA_OPERATION_STATUS AS ENUM('UNKNOWN', 'RECEIVED', 'APPLIED', 'REJECTED');
CREATE DOMAIN ATALA_OPERATION_ID AS BYTEA
CHECK (
  LENGTH(VALUE) = 32
);

CREATE TABLE atala_operations(
    signed_atala_operation_id ATALA_OPERATION_ID NOT NULL,
    atala_object_id ATALA_OBJECT_ID NOT NULL,
    atala_operation_status ATALA_OPERATION_STATUS NOT NULL,
    -- constraints
    CONSTRAINT signed_atala_operation_id_pk PRIMARY KEY (signed_atala_operation_id),
    CONSTRAINT atala_object_id_fk
        FOREIGN KEY (atala_object_id)
        REFERENCES atala_objects (atala_object_id)
);
