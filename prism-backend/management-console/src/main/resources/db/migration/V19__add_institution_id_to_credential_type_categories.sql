ALTER TABLE credential_type_categories
    ADD COLUMN institution_id UUID,
    ADD CONSTRAINT credential_type_categories_institution_id
        FOREIGN KEY (institution_id)
            REFERENCES participants (participant_id)
            ON DELETE CASCADE
