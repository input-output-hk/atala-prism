CREATE TABLE messages (
  id UUID NOT NULL,
  received_at TIMESTAMPTZ NOT NULL,
  connection UUID NOT NULL,
  recipient UUID NOT NULL,
  sender UUID NOT NULL,
  content BYTEA NOT NULL,
  CONSTRAINT messages_id_pk PRIMARY KEY (id),
  CONSTRAINT messages_connection_fk FOREIGN KEY (connection) REFERENCES connections (id),
  CONSTRAINT messages_recipient_fk FOREIGN KEY (recipient) REFERENCES participants (id),
  CONSTRAINT messages_sender_fk FOREIGN KEY (sender) REFERENCES participants (id)
);

CREATE INDEX messages_received_at_index ON messages USING BTREE (sender);
CREATE INDEX messages_connection_index ON messages USING BTREE (connection);
CREATE INDEX messages_recipient_index ON messages USING BTREE (recipient);
CREATE INDEX messages_sender_index ON messages USING BTREE (sender);
