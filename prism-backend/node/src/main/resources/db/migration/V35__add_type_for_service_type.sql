
CREATE TYPE SERVICE_TYPE AS ENUM('LinkedDomains', 'DIDCommMessaging', 'CredentialRegistry');

ALTER TABLE services
    ALTER COLUMN type SET DATA TYPE SERVICE_TYPE USING type::service_type