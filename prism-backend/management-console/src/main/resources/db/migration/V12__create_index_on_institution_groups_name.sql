CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX institution_groups_name_index ON institution_groups USING gin (name gin_trgm_ops)