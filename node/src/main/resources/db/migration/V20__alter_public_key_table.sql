ALTER TABLE public_keys
DROP COLUMN x;

ALTER TABLE public_keys
DROP COLUMN y;

ALTER TABLE public_keys
RENAME COLUMN xCompressed
TO compressed;

ALTER TABLE public_keys
ALTER COLUMN compressed
SET NOT NULL;