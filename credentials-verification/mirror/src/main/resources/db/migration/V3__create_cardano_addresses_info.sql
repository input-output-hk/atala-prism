
CREATE TABLE cardano_addresses_info(
  address TEXT NOT NULL PRIMARY KEY,
  network TEXT NOT NULL,
  connection_token TEXT NOT NULL,
  registration_date TIMESTAMPTZ NOT NULL,
  message_id TEXT NOT NULL,
  CONSTRAINT cardano_addresses_info_connection_token_fk FOREIGN KEY (connection_token) REFERENCES connections (token),
  CONSTRAINT cardano_addresses_info_message_id_unique UNIQUE (message_id)
);

CREATE INDEX cardano_addresses_info_connection_token_index ON cardano_addresses_info (connection_token);
CREATE INDEX cardano_addresses_info_registration_date_index ON cardano_addresses_info (registration_date);
