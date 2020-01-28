begin;
delete from messages;
delete from connections;
delete from connection_tokens;
delete from credentials;
delete from holder_public_keys;
delete from students;
delete from payments;
delete from issuers;
delete from participants;
delete from store_individuals;
delete from store_users;
delete from stored_credentials;

commit;
