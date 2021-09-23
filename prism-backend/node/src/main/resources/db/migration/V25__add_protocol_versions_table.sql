CREATE DOMAIN BLOCK_NO AS INT
CHECK (
  VALUE >= 0
);

create table protocol_versions(
  major_version non_negative_int_type not null,
  minor_version non_negative_int_type not null,
  version_name varchar(256) null,
  effective_since BLOCK_NO not null,
  published_in transaction_id not null,
  is_effective bool not null,
  proposer_did id_type not null,

  CONSTRAINT protocol_version_pk PRIMARY KEY  (major_version, minor_version)
);
