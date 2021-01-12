-- In order to decouple management console from connector, we are moving connection status from
-- contacts table to connections table.
ALTER TABLE connections
    ADD COLUMN status CONTACT_CONNECTION_STATUS_TYPE NULL;

UPDATE connections
SET status = contacts.connection_status
FROM contacts
WHERE contacts.connection_id = connections.id;

UPDATE connections
SET status = 'INVITATION_MISSING'::CONTACT_CONNECTION_STATUS_TYPE
WHERE status IS NULL;

ALTER TABLE connections
    ALTER COLUMN status SET NOT NULL;
