-- the last time the credential was shared to the related contact which belongs
-- to the published_credentials because a non-published credential shouldn't be shared
ALTER TABLE published_credentials
  ADD COLUMN shared_at TIMESTAMPTZ NULL DEFAULT NULL;
