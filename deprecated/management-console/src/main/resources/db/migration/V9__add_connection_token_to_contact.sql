
TRUNCATE contacts CASCADE;

ALTER TABLE contacts
ADD COLUMN connection_token TEXT NOT NULL;
