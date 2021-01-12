
CREATE TABLE connector_message_offset(
  id INT NOT NULL PRIMARY KEY,
  message_id TEXT NOT NULL,
  CONSTRAINT connector_message_offset_one_row CHECK (id = 1)
);
