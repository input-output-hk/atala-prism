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
With the current implementation of node database, this is in fact the only possible option, because, for DIDs for example, we are storing the latest state of the did after all operations have been applied, 
we are not storing every state of the DID that it went through as some sort of the history of the DID, so when the rollback happens, even if we know how many blocks exactly have been rolled back and what is the latest block currently in Cardano blockchain, 
we can not roll back the state of our node database exactly to this point in time, because we don’t have the history stored.
Every time the DID is updated, it literally is updated so the state of the DID is overridden, if the rollback happened, we can not know what is the latest valid state of the DID. 
In order to have a way to roll back the state of the DID exactly to a specific point in time, instead of storing the state of a single DID and updating it once new operations are found in the blockchain,
we could store every state of every DID and associate Cardano block number on which this update has happened, when users are retrieving DID, they would get the latest state of it, but we would keep the history in case a rollback is happening or for any other reason, 
the functionality to display the history of how DID or VC has changed would be a nice feature.


Another problem that needs to be addressed is the Automatic synchronization of the node database with Cardano blockchain in case a rollback is happening, for this, we need functionality to automatically detect if the rollback has happened and then run one of the aforementioned operations.
Ideally, the Cardano node would issue an event if there is a rollback and provide information such as which block the blockchain is rollbacked to (so which block number is currently the latest block on the chain), 
then we would listen to this event and trigger resynchronization. In case such functionality is not available, we could store the index of the latest block every time we are updating the state of our internal node database, and then periodically query the Cardano node to see which block is the latest,
if the index of the latest block is less then what we have right now, that would mean that rollback has happened. This second approach is not ideal and feels hacky.
In both cases, we need to trigger resynchronization only if rollback happened by more blocks than we wait for confirmations, currently 31 blocks.


## Query results while synchronization is happening

Read queries should return an error stating that data is unavailable due to synchronization being run at the moment, read operations could be queued if those are CREATE and not UPDATE, 
because UPDATE usually happens based on the current state, and if synchronization is happening, that would mean that user that is performing an update might be updating the state that is not valid in the first place. 


## Other considerations

We need to have answers to the following questions in order to move forward

- How much time would it take to re-index the node database starting from the genesis block? is it a lot? if so then we need to strongly consider implementing the “storing history” functionality I’ve mentioned above as a second solution for rollbacks because the more chain grows, the longer it is going to take to re-index the node database starting from the very beginning, and every time there is a rollback (more then 31 blocks), this whole operation needs to be run.
- Is there a valid way we can know there has been a rollback? do we need to implement our own way of doing it like storing the latest index and then checking the current index of the blockchain is less?
  - Are there any pitfalls with this approach that I’m not seeing?
- Which tables would need to be changed in case of rollback (assuming we not truncating our database entirely and only changing it to match the current state of the blockchain)? In any case, all this data needs to be associated with the Cardano block it was derived from, in order to know which of this data is valid (has not been included in the rollback) and which one is not (has been included in the rollback)

