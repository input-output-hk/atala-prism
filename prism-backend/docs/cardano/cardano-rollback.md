## Description of the problem

Atala node is a second layer ledger on top of Cardano blockchain, the node maintains an internal database which it uses to query results for all operations,
this is done for efficiency and speed, as opposite of querying the blockchain every time a user needs to retrieve the current state of a DID document for example. 
We can think of a node database as some sort of a cache for Cardano blockchain so that we don’t have to query the whole blockchain and derive the latest state of the DID every time, 
we have a synchronizer that periodically queries that blockchain (when enough blocks have been confirmed) and updates the state of the did in internal node database.
The problem may arise if the Cardano blockchain state has been changed backward (a.k rollback), in this case, the state of the node database, 
which is supposed to represent the state of Cardano blockchain but in a more manageable manner in which Prism users are interested in, is incorrect, 
because the source of truth is Cardano blockchain and node database should reflect the source of truth.

## Possible solutions to the problem

The first and most straightforward solution is if the rollback happened to completely truncate the node database and let the synchronizer re-index the internal node database starting from the genesis block,
which is defined as the first block where Atala operation has been included. this block is hardcoded in node config. 
With the current implementation of node database, Another solution would be to unapply every operation in reverse order one by one up until the last operation that has not been rolled back in the blockchain, this is possible but more involved.

In order to implement second solution the following needs to be done.

**1. Associate every operation with the index of cardano block in which it was included**

This needs to be done so that we have the information on whether or not this operation needs to be unapplied. 
When operation is being applied to the state, that means that operation is already in the blockchain in some cardano block, index of this block needs to be associated with the operation

**2. Implement a service that will detect a rollback**

Since we are constantly checking for a new block in order to apply the state changes, we can also store the last index of cardano block we have seen, and every time service check for new blocks,
it can also compare the latest block it has stored to the latest block in the blockchain and detect a rollback that way. service should account for number of blocks to wait to confirm transaction, because if rollback was done with fewer blocks than that,
than no need to trigger an unapply procedure

**3. Implement rollback procedure**

Procedure should receive an index of cardano block to which it was rolled back to.
The procedure will unapply every operation in reverse order, starting from the last one until the one that blockchain was rolled back to

**4. Implement `unapplyState` function for every operation that changes the state, these are currently**
```protobuf
message AtalaOperation {
// The actual operation.
   oneof operation {
// Used to create a public DID.
        CreateDIDOperation create_did = 1;
        
        // Used to update an existing public DID.
        UpdateDIDOperation update_did = 2;
        
        // Used to issue a batch of credentials.
        IssueCredentialBatchOperation issue_credential_batch = 3;
        
        // Used to revoke a credential batch.
        RevokeCredentialsOperation revoke_credentials = 4;
        
        // Used to announce new protocol update
        ProtocolVersionUpdateOperation protocol_version_update = 5;
    };
}
```
`CreateDIDOperation` - inserts a did into did_data table and public key into `public_keys` table, just remove them. 

`UpdateDIDOperation` - has two values `AddKeyAction` and `RevokeKeyAction`. Which one was used, this inforamtion is stored inside Atala object associated with the operation
So it can be unapplied.

`IssueCredentialBatchOperation` - This operation inserts a record into `credential_batches` table, so just remove them.

`RevokeCredentialsOperation` - Inserts into `revoked_credentials` table, just remove it.

`ProtocolVersionUpdateOperation` -  We have the full history, we’ll just remove the version that operations has inserted.


Every operation that is unapplied should be marked as "RECEIVED", this way, they can be submitted into the blockchain again and re-applied to the state. 



## Query results while synchronization is happening

Read queries will return the data based on current state of the database, but if this happens while rollback is taking place, response will have a flag communicating to the client that this response might not be accurate.
Update queries should be rejected because they are usually based on some state that might be incorrect in the first place.
Insert queries will be queued like normally.


