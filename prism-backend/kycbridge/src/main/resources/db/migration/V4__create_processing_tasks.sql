CREATE TABLE processing_tasks(
    id UUID NOT NULL PRIMARY KEY,
    state TEXT NOT NULL,
    owner UUID NULL,
    last_change TIMESTAMPTZ NOT NULL,
    last_action TIMESTAMPTZ NOT NULL,
    next_action TIMESTAMPTZ NOT NULL,
    data JSONB
);
