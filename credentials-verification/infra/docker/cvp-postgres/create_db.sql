CREATE DATABASE connector;
CREATE USER connector WITH ENCRYPTED PASSWORD 'connector';
GRANT ALL PRIVILEGES ON DATABASE connector TO connector;

CREATE DATABASE node;
CREATE USER node WITH ENCRYPTED PASSWORD 'node';
GRANT ALL PRIVILEGES ON DATABASE node TO node;

CREATE DATABASE demo;
CREATE USER demo WITH ENCRYPTED PASSWORD 'demo';
GRANT ALL PRIVILEGES ON DATABASE demo TO demo;
