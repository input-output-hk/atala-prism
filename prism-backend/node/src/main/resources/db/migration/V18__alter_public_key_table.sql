ALTER TABLE public_keys
ADD COLUMN xCompressed bytea NULL;

ALTER TABLE public_keys
ADD CONSTRAINT x_compressed_length CHECK (LENGTH(xCompressed) = 33);
