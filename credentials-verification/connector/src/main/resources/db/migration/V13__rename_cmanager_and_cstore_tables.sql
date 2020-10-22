ALTER TABLE students
  RENAME TO issuer_subjects;

ALTER TABLE store_users
  RENAME TO verifiers;

ALTER TABLE store_individuals
  RENAME TO verifier_holders;

ALTER TABLE credentials
   DROP CONSTRAINT credentials_student_id_fk,
   ADD CONSTRAINT credentials_subject_id_fk FOREIGN KEY (student_id) REFERENCES issuer_subjects (student_id);

DROP INDEX students_created_on_index;
DROP INDEX students_connection_token_index;

CREATE INDEX subjects_created_on_index ON issuer_subjects USING BTREE (created_on);
CREATE INDEX subjects_connection_token_index ON issuer_subjects USING BTREE (connection_token);
