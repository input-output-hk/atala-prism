# Connector E2E

Tickets

1. **Update Holders local database to include`key_agreement_derivation_path`, `recipient_pk` and `secret`**

   Description:
   Right now mobile wallets store list of contacts in their local database, this database should be extended with aforementioned keys.

   Codebase - Android app and IOS app. (there will be 2 tickets in Jira for each)


2. **Update Issuers database to include `key_agreement_derivation_path`, `recipient_pk` ,`secret`, `connection_id`**

   Description:
   Right now, Issuer stores a list of contacts in management_console_db/contacts Postgresql database, we should add a migration that adds aforementioned fields to the table.

   Codebase - Management console backend


3. **Implement step 1, 2 and 3 of [key agreement protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps)**

   Description:

   Right now before issuer presents a QR code with connection token, it first requests to create a token in connector database by issuing `GenerateConnectionToken` GRPC request, and then presents a QR code containing this token as the response arrives. This process should be extended by also creating new key derivation path and random 256 bit secret in issuers database. The contact will already be created by that time (it has to be) so this contact entry should be updated. Once all of this is done, a QR code with all 3 of this pieces of information has to be presented via QR code.

   Codebase - Management console frontend
   Blocked by - 2, 4


4. **Add a counter on issuers side that will be incremented every time a new connection is established**

   Description:

   This should be done the same way it is done on mobile wallet side, the counter will be used to generate a key derivation path for key agreement key

   Codebase - Web wallet


5. **Implement part 4 and 5 of [key agreement protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps)**

   Description:

   Right now mobile wallet scans the QR code and obtains a token, then stores a new contact and generates all necessary information for them, since QR code now will include not only QR code but also secret and public key, holder should parse the QR code accordingly and stores this information in association with a new contact created.

   Codebase - Android app and IOS app. (there will be 2 tickets in Jira for each)
   Blocked by - 1


6. **Implement part 6 of the [key agreement protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps)**

   Description:

   When the wallet accepts a connection, it responds with `addConnectionFromToken`, which finalizes the process of establishing the connection via the connector. After that another request should be sent from the holder to the issuer, which will contain holders public key (derived from `key_agreement_derivation_path` ) and MAC of the message. This should be message of type `AuthenticatedPublicKey` from the [docs](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#grpc-messages)

   Codebase - Android app and IOS app. (there will be 2 tickets in Jira for each)
   Blocked by - 7


7. **Add MAC calculation functionality to SDK**

   Description:

   crypto module should be extended with MAC calculation functionality. the function should take 2 parameters, data which MAC should be calculated and secret to calculate the mac

   Codebase - SDK


8. **Implement part 7 and 8 of the [key agreement protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps)**

   Description:

   Issuer should parse the received message, validate MAC and update the database (management_console_db/contacts) with received public key, in case MAC is authentic. In order to validate mac Issuer will need a secret, Issuer will be able to obtain secret by querying the row by connection_id that is attached to the message, or by connection token that can be retrieved by connection_id

   Codebase - Management console frontend
   Blocked by - 7, 9


9. **Add functionality management console to query messages to the issuer via connector**

   Description:

   Right now, mobile apps are periodically queuing for new messages from connector that are sent to them, that is how they are able to "receive" messages, management console should be able to do the similar thing because issuer is receiving messages from the holder as well, such functionality is not present on management console side right now and should be implemented.

   new messages should be queries only for new contact that have started the key agreement process but have not finished it yet, that will be contact that has `secret` and `key_agreement_derivation_path`, which means QR code has been shared to them, but don't have `recipient_pk` and `connection_id` in database yet, which means he is looking for the new message from the holder.

   Codebase - Management console frontend
   Blocked by - 2


10. **Add ECIES to SDK**

    Description:

    ECIES should be implemented in JVM, Js, Android and IOS (there will be 4 different tickets)
    There should be support for encryption and decryption

    Codebase - SDK


11. **Extend prism-api connector module with `EncryptedAtalaMessage`**

    Description:

    SDK should have a functionality to `EncryptedAtalaMessage`, which includes encrypting and signing `AtalaMessage` with provided public and private keys

    SDK should also be able deconstruct `EncryptedAtalaMessage`, which includes decrypting it and extracting raw `AtalaMessage` out of it, as well as verifying a signature.

    Codebase - SDK
    Blocked by - 10


12. **Implement part 3,4,5 and 6 of [encryption protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps-1)**

    Description:

    Utilize SDK to construct and send `EncryptedAtalaMessage`

    Codebase - Management console frontend
    Blocked by - 11


13. **Extend Issuer database to add `rValue`**

    Description:

    Add a migration to connector backend that will add the field `rValue` to messages table, this value will be nullable and will be used to decrypt this messages

    Codebase - Prism backend (connector)
    Blocked by - 12


14. **Add functionality to backend to parse `EncryptedAtalaMessage` and store it into database**

    Description:

    since `EncryptedAtalaMessage` is a oneof of `AtalaMessage`, there already is a handler that accepts a message and stores it into database, this functionality should be expended to accommodate new type of message, including extracting `rValue` and storing it separately

    Codebase - Prism backend (connector)
    Blocked by - 13


15. **implement part 8,9,10 and 11 of [encryption protocol](https://github.com/input-output-hk/atala-prism/blob/master/prism-backend/docs/connector/e2e-encryption.md#steps-1)**

    Description:

    Mobile wallet should parse `EncryptedAtalaMessage` extract `AtalaMessage` and handle them in the same way they are handling other messages of type `AtalaMessage`

    Codebase - Android app and IOS app. (there will be 2 tickets in Jira for each)
    Blocked by - 11