
CREATE TYPE PAYMENT_STATUS_TYPE AS ENUM(
    'CHARGED', -- the user has been charged
    'FAILED' -- charging the user failed
);

CREATE TABLE payments (
  payment_id UUID NOT NULL,
  participant_id UUID NOT NULL,
  nonce TEXT NOT NULL, -- braintree payments specific field to do a payment
  amount DECIMAL(20, 4), -- 4 decimals should be enough for any currency for a while
  created_on TIMESTAMPTZ NOT NULL,
  status PAYMENT_STATUS_TYPE NOT NULL,
  failure_reason TEXT NULL, -- present when status = FAILED
  CONSTRAINT payments_id_pk PRIMARY KEY (payment_id),
  CONSTRAINT payments_participant_id_fk FOREIGN KEY (participant_id) REFERENCES participants (id),
  CONSTRAINT payments_nonce_unique UNIQUE (nonce)
);

CREATE INDEX payments_participant_id_index ON payments USING BTREE (participant_id);
CREATE INDEX payments_nonce_index ON payments USING BTREE (nonce);
