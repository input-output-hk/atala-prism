-- add the ID credential issuer
INSERT INTO participants (id, tpe, name, did) VALUES ('091d41cc-e8fc-4c44-9bd3-c938dcf76dff', 'issuer', 'Department of Interior, Republic of Redland', 'did:test:091d41cc-e8fc-4c44-9bd3-c938dcf76dff');

-- add the degree credential issuer
INSERT INTO participants (id, tpe, name, did) VALUES ('6c170e91-92b0-4265-909d-951c11f30caa', 'issuer', 'Airside University', 'did:test:6c170e91-92b0-4265-909d-951c11f30caa');

-- storage for credential state machines.
CREATE TABLE intdemo_credential_status(
                              token TEXT NOT NULL,
                              status INT NOT NULL,
                              CONSTRAINT intdemo_credential_status_pk PRIMARY KEY (token));

-- personal data storage for the id credential issuer.
CREATE TABLE intdemo_id_personal_info(
                              token TEXT NOT NULL,
                              first_name TEXT NOT NULL,
                              date_of_birth DATE NOT NULL,
                              CONSTRAINT intdemo_id_personal_info_pk PRIMARY KEY (token));
