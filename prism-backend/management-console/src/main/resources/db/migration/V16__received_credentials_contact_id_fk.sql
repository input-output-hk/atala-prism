-- there was a bug which used an invalid contact_id while storing data in the received_credentials table
-- which could have been prevented by having a fk

-- first, all received_credentials need to be deleted because we can't fix the contact_id references
DELETE FROM received_credentials;

-- then, we add the fk
ALTER TABLE received_credentials
  ADD CONSTRAINT received_credentials_contact_id_fk FOREIGN KEY(contact_id) REFERENCES contacts(contact_id);
