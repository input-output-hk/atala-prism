CREATE TYPE credential_type_category_state as ENUM ('DRAFT', 'READY', 'ARCHIVED');

CREATE TABLE credential_type_categories
(
    credential_type_category_id UUID PRIMARY KEY,
    name                        TEXT                           NOT NULL,
    state                       credential_type_category_state NOT NULL
);