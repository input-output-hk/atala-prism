
-- This migration assumes that tables services and services_endpoints are empty
DROP TABLE service_endpoints;

ALTER TABLE services
    ADD COLUMN service_endpoints TEXT NOT NULL default '';

ALTER TABLE services
    ALTER COLUMN service_endpoints DROP DEFAULT;

