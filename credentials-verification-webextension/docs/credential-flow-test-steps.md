# Testing steps

This file documents the steps to:
1. Run the browser wallet locally
1. Talk to the PrismSdk through the browser console
1. Issue a credential using the browser SDK
1. Verify the credential issued

## Setting up

### Environment notes

The steps in this document were replicated in an Ubuntu environment with the following versions

- Postgres version: 12.2
- psql version: 12.3
- envoy: 1.12.1

### Set up steps

We will build everything from scratch.

1. First, we will start the Postgres server. If you already have this up and running, you can skip this step.
   ``` 
   $ docker run -it --rm -e POSTGRES_DB=connector_db -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres 
   ```
   
   The above command will start a docker Postgres container with a database necessary for the connector.
   Given that we will also need a database for the node, open a postgres client and create the needed database as follows:
   
   ``` 
   $ psql connector_db \
       -U postgres \
       -h localhost \
       -p 5432
   ```
   This will open an sql shell connected to the docker container.
   Execute the query:
   
   ```
   connector_db=# CREATE DATABASE node_db;
   ```
   and keep the sql shell open because we will need it for next steps.

2. Now, we should run the node, connector and envoy locally.

   You will need three more terminals/tabs to run this. 
   Run the commands in sequence (i.e. wait for each one to finish before running the next one).

   **NOTE:** If you are a *linux* user, you could alternatively go to `credentials-verification-webextension`, and run
   `$ ./run_local.sh`, and move directly to step 4. 
   
   Connector
   ```
   [terminal 1]
   atala$ cd credentials-verification
   credentials-verification$ sbt "connector/run"
   ```
   
   Node
   ```
   [terminal 2]
   atala$ cd credentials-verification
   credentials-verification$ sbt "node/run"
   ```
   
   Envoy
   
   **NOTE FOR MAC USERS:** There seems to be a problem to run envoy on docker.
   There is an `envoy.yaml` file in `credentials-verification-web/envoy` that instruct in comments an attempt to
   fix the problem. Some Mac users suggested to install envoy locally and avoid docker completely. 
   ```
   [terminal 3]
   atala$ cd credentials-verification-web/envoy
   envoy$ docker run --rm -ti --net=host -v $PWD/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.12.1
   ```
   
3. Now it is time to compile the web extension (the wallet)
   
   ``` 
   atala$ cd credentials-verification-webextension
   credentials-verification-webextension$ sbt chromeUnpackedFast
   ```

4. Now let's install the wallet on chrome. Open chrome and go to [chrome://extensions/](chrome://extensions/)

Activate the developer mode (top right corner of the page) and click on `Load unpacked` (top left visible in developer 
mode), in the dialog opened go to `atala/credentials-verification-webextension/target/chrome`, select `unpacked-fast`
folder and click `Open`. Now the wallet should be found in your plugins. Do not open the wallet yet.

At this point, you have all the needed components up to run the wallet locally. 
We now move to the testing part.

## Fun part

Now it is time to run the actual test.

1. In chrome, Go to [https://atalaprism.io/](https://atalaprism.io/)
1. Now open the developer console (usually CTRL + SHIFT + i in non-Mac systems)
1. In the console you can interact with the PrismSdk by calling the methods in `window.prism`

   To test that everything is connected, enter this command in the console:
   ```
   > window.prism.getWalletStatus()
   ```
   You should get something like
   ```
   Promise {<pending>}
   main-bundle.js:110650 BackgroundAPI: Sending command {"GetWalletStatus":{}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Right":{"value":{"status":{"Missing":{}}}}}    
   ```

1. Now, open the wallet, by clicking on `Extensions -> ATALA Browser Wallet`, and click on `Register`. Complete the form
to create an **Issuer**. In this example we will use the organization name `testOrg` (you do not need to upload a logo 
and we will not need to remember any other data apart from the organization name used)

   If you now check the wallet status in the console (`window.prism.getWalletStatus()`) you should get something like
   ```
   Promise {<pending>}
   main-bundle.js:110650 BackgroundAPI: Sending command {"GetWalletStatus":{}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Right":{"value":{"status":{"Unlocked":{}}}}}
   ```


1. In the console, run the login command:
   ``` 
   > window.prism.login()
   Promise {<pending>}
   main-bundle.js:110650 BackgroundAPI: Sending command {"GetUserSession":{}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Right":{"value":
     {"sessionId":"63dc6e84-37cd-4117-8a3c-0b733cc350e4","name":"testOrg","role":"Issuer","logo":[]}}}
   ``` 
1. Extract the session id and define in the console again:

   ```
   > sessionId = "63dc6e84-37cd-4117-8a3c-0b733cc350e4"
   "63dc6e84-37cd-4117-8a3c-0b733cc350e4"
   ```
   it will be useful for future commands.
   
1. Now move to the sql shell we open to connect with the postgres docker container.
   We need to first obtain the `issuer_id` defined for the issuer we created.

   ``` 
   connector_db=# SELECT name,id FROM participants ;
                     name                   |                  id                  
   -----------------------------------------+--------------------------------------
    Issuer 1                                | c8834532-eade-11e9-a88d-d8f2ca059830
    Verifier 1                              | f424f42c-2097-4b66-932d-b5e53c734eff
    Holder 1                                | e20a974e-eade-11e9-a447-d8f2ca059830
    Metropol City Government                | 091d41cc-e8fc-4c44-9bd3-c938dcf76dff
    University of Innovation and Technology | 6c170e91-92b0-4265-909d-951c11f30caa
    Decentralized Inc.                      | 12c28b34-95be-4801-951e-c775f89d05ba
    Verified Insurance Ltd.                 | a1cb7eee-65c1-4d7f-9417-db8a37a6212a
    testOrg                                 | db4e7c01-3912-4be9-b41b-7e0e4a79ce4e
   ```
   In our case is the id `db4e7c01-3912-4be9-b41b-7e0e4a79ce4e`

1. For future use, go to the browser console an `id` variable with the uuid extracted from the 
   previous step. In this example, it would be:

   ```
   > id = "db4e7c01-3912-4be9-b41b-7e0e4a79ce4e"
   ```

1. In order to publish a credential, we need to have a credential in the cmanager. 
   In order to add a credential in the cmanager, we need to assign to it a `subject` and an `issuer_group`
   Given the backend validations and db constraints, we need to create all the rows in the databases in a way 
   consistent to the issuer we registered with the browser wallet.
   So, we created this Scala function that, given the `issuer_id` (retrieved in the previous step) will return to you 
   the queries you need to run in the sql shell.
   The Scala function is:

   ```
   def queries(uuid: String): String = 
    s"""
       |INSERT INTO issuer_groups (group_id,issuer_id,name) VALUES ('$uuid','$uuid', 'Test');
       |INSERT INTO contacts (contact_id , external_id , created_at , connection_status , created_by , contact_data)
       |VALUES ('$uuid', '$uuid', now(), 'CONNECTION_MISSING','$uuid','{}');
       |INSERT INTO contacts_per_group (group_id, contact_id, added_at) 
       |VALUES ('$uuid', '$uuid', now());
       |INSERT INTO credentials
       |(credential_id,issuer_id,subject_id,group_name,created_on,credential_data)
       |VALUES ('$uuid','$uuid','$uuid','Test',now(),'{}');
       |""".stripMargin
   ```
   Following our example, we open a REPL and do as follows 
   (use the uuid you obtained in the step below, here we use `db4e7c01-3912-4be9-b41b-7e0e4a79ce4e`):

   ```
   $ scala
   scala> def queries(uuid: String): String =
            s"""
            |INSERT INTO issuer_groups (group_id,issuer_id,name) VALUES ('$uuid','$uuid', 'Test');
            |INSERT INTO contacts (contact_id , external_id , created_at , connection_status , created_by , contact_data)
            |VALUES ('$uuid', '$uuid', now(), 'CONNECTION_MISSING','$uuid','{}');
            |INSERT INTO contacts_per_group (group_id, contact_id, added_at)
            |VALUES ('$uuid', '$uuid', now());
            |INSERT INTO credentials
            |(credential_id,issuer_id,subject_id,group_name,created_on,credential_data)
            |VALUES ('$uuid','$uuid','$uuid','Test',now(),'{}');
            |""".stripMargin
   queries: (uuid: String)String

   scala> print(queries("db4e7c01-3912-4be9-b41b-7e0e4a79ce4e"))

   INSERT INTO issuer_groups (group_id,issuer_id,name) VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'Test');
   INSERT INTO contacts (contact_id , external_id , created_at , connection_status , created_by , contact_data)
   VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', now(), 'CONNECTION_MISSING','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','{}');
   INSERT INTO contacts_per_group (group_id, contact_id, added_at)
   VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', now());
   INSERT INTO credentials
   (credential_id,issuer_id,subject_id,group_name,created_on,credential_data)
   VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','Test',now(),'{}');
   ```
  
   **NOTE:** For simplicity all the needed ids (i.e. `credential_id`, `contact_id`, `group_id`) have the same value as 
   the `issuer_id`.

1. Run the queries in the sql shell

   ```
   connector_db=# INSERT INTO issuer_groups (group_id,issuer_id,name) VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'Test');
   INSERT 0 1
   connector_db=# INSERT INTO contacts (contact_id , external_id , created_at , connection_status , created_by , contact_data)
   connector_db=# VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', now(), 'CONNECTION_MISSING','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','{}');
   INSERT 0 1
   connector_db=# INSERT INTO contacts_per_group (group_id, contact_id, added_at)
   connector_db=# VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', 'db4e7c01-3912-4be9-b41b-7e0e4a79ce4e', now());
   INSERT 0 1
   connector_db=# INSERT INTO credentials
   connector_db=# (credential_id,issuer_id,subject_id,group_name,created_on,credential_data)
   connector_db=# VALUES ('db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','db4e7c01-3912-4be9-b41b-7e0e4a79ce4e','Test',now(),'{}');
   INSERT 0 1
   ```

1. Now we are ready to publish a credential using the PrismSdk from the browser console.

   Go to the browser console, and execute:
   
   ```
   > window.prism.requestSignature(sessionId, '{ "id" : "' + id + '", "properties": { "degree" : "CS Degree" } }')
   ```
   a pop-up (or new tab) will appear requesting authorization to sign a request. The pop-up, at the time of this 
   writing, displays the `id` used in a green button. Click on the button and then go back to the console.
   You should see something like:
   
   ``` 
   BackgroundAPI: Sending command {"RequestSignature":{"sessionId":"63dc6e84-37cd-4117-8a3c-0b733cc350e4","subject":{"id":"db4e7c01-3912-4be9-b41b-7e0e4a79ce4e","properties":{"degree":"CS Degree"}}}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Right":{"value":{}}}
   ```
   
1. To verify that the issuance was correct you can check logs in the connector and node terminals.
   We will now go again to the sql shell and type:
   
   ``` 
   connector_db=# SELECT * FROM published_credentials ;
   ```
   If everything worked as expected you should see a row like this one
   
   ```
               credential_id             |                        node_credential_id                        |                          operation_hash                          |                                                                                                                                 encoded_signed_credential                                                                                                                                 |           stored_at           
   --------------------------------------+------------------------------------------------------------------+------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------
    db4e7c01-3912-4be9-b41b-7e0e4a79ce4e | d232ab66208814875fe7fac62ee0dece6ffd03132ffdfd65916e8e15df046402 | d232ab66208814875fe7fac62ee0dece6ffd03132ffdfd65916e8e15df046402 | eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-T | 2020-07-22 19:16:28.127975+00
   ```
   
   We care about the `encoded_signed_credential` column, which represents the credential we issued. In our case:
   
   `eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-T`
   
   The credential is currently conformed by two base64URL encoded parts separated by a dot (".").
   The first part is the credential without the signature and the second part is a signature.
   We can decode the first part (`eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=`)
   [here](https://base64.guru/standards/base64url/decode) to obtain something like:
   
   ```
   {"issuer":"did:prism:498629716f863b6ce282930f77c5f82bdc1b9fe57c4341ca0df37860cf2ee25e","keyId":"master0","claims":{"degree":"CS Degree"}}
   ```
1. Define in the console the following variable to represent your encoded signed credential. In this example, this would be:

   ``` 
   > credential = 'eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-T'
   ```
    
1. Let us finish the test by verifying the credential using the PrismSdk through the console. 
   Execute:
   
   ``` 
   > window.prism.verifySignedCredential(sessionId, credential)
   Promise {<pending>}
   main-bundle.js:110650 BackgroundAPI: Sending command {"VerifySignedCredential":{"sessionId":"63dc6e84-37cd-4117-8a3c-0b733cc350e4","signedCredentialStringRepresentation":"eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-T"}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Right":{"value":{"result":true}}}
   ```
   
   The response `{"Right":{"value":{"result":true}}}` indicates that the credential is valid.
   You can modify the encoded signed credential string and execute the command again. You will receive an error instead 
   of the `true` response. For example, if we change the last `T` in the string for a `U`, we get:
    
   ```
   > credential = "eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-U"
   "eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-U"
   
   > window.prism.verifySignedCredential(sessionId, credential)
   Promise {<pending>}
   main-bundle.js:110650 BackgroundAPI: Sending command {"VerifySignedCredential":{"sessionId":"63dc6e84-37cd-4117-8a3c-0b733cc350e4","signedCredentialStringRepresentation":"eyJpc3N1ZXIiOiJkaWQ6cHJpc206NDk4NjI5NzE2Zjg2M2I2Y2UyODI5MzBmNzdjNWY4MmJkYzFiOWZlNTdjNDM0MWNhMGRmMzc4NjBjZjJlZTI1ZSIsImtleUlkIjoibWFzdGVyMCIsImNsYWltcyI6eyJkZWdyZWUiOiJDUyBEZWdyZWUifX0=.MEYCIQCFqUMF099bwKbqcm676uD4q13M1CIa1UI1e4Ydw3TiiQIhAMIYBcmOSTBHOSGjCv2qzoDoB6SGFbdUkj96HatMQD-U"}}
   main-bundle.js:110650 BackgroundAPI: Received response {"Left":{"value":"2: Unknown credential_id: f38a58293bdc3e6dc93f61ce41751af2fb3b57724133335913c47c69580a5f22"}}
   ``` 
    
   the error occurs because modifying the string leads to an unknown credential id for the node. Once we add revocation and 
   error handling, this error message will be clearer. 

Those are all the steps so far.
