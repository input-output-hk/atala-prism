begin;
delete from messages;
delete from connections;
delete from connection_tokens;
delete from credentials;
delete from holder_public_keys;
delete from issuer_subjects;
delete from payments;
delete from issuer_groups;
delete from issuers;
delete from participants;
delete from verifier_holders;
delete from verifiers;
delete from stored_credentials;

commit;
