ALTER TABLE credential_types
    ADD COLUMN credential_type_category_id UUID,
    ADD CONSTRAINT credential_type_category_id_fk
        FOREIGN KEY (credential_type_category_id)
            REFERENCES credential_type_categories (credential_type_category_id)
            ON DELETE SET NULL
