CREATE TABLE cardano_wallet_addresses(
  address TEXT NOT NULL PRIMARY KEY,
  wallet_id UUID NOT NULL,
  sequence_no INT NOT NULL,
  used_at TIMESTAMPTZ,
  CONSTRAINT cardano_wallet_addresses_wallet_id_fk FOREIGN KEY (wallet_id) REFERENCES cardano_wallets (id)
);

CREATE INDEX cardano_wallet_addresses_wallet_id_index ON cardano_wallet_addresses (wallet_id);
