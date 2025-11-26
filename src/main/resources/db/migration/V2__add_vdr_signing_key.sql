-- Add VDR signing key usage to support signing PRISM VDR driver operations
ALTER TYPE public.key_usage ADD VALUE IF NOT EXISTS 'VDR_SIGNING_KEY';
