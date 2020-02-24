-- add the ID credential issuer
INSERT INTO participants (id, tpe, name, did) VALUES ('091d41cc-e8fc-4c44-9bd3-c938dcf76dff', 'issuer', 'Department of Interior, Republic of Redland', 'did:test:091d41cc-e8fc-4c44-9bd3-c938dcf76dff');

-- storage for the ID credential state machine.
CREATE TABLE intdemo_credential_status(
                              token TEXT NOT NULL,
                              status INT NOT NULL,
                              CONSTRAINT intdemo_credential_status_pk PRIMARY KEY (token));
