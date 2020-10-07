
CREATE TYPE CONNECTION_STATE AS ENUM ('INVITED', 'CONNECTED', 'REVOKED');

CREATE TABLE connections(
  token TEXT NOT NULL PRIMARY KEY,
  id UUID,
  state CONNECTION_STATE NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX connections_updated_at_id_index ON connections USING BTREE (updated_at, id);
