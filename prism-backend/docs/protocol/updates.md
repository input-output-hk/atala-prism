# Update mechanism

## Context

We have designed a protocol description of PRISM that guided our implementation for the PRISM node. 
We are now approaching the first deployments in production systems, and we need to consider the challenges that this bring.
In particular, the distributed nature of our system requires special care at the time of updating the node. Updates can have different consequences:
1. If we make internal optimizations in the PRISM node, users could opt to delay upgrading the version as no semantic change will come and the 
   infrastructure will keep working.
2. When a Cardano hard fork approaches, all PRISM node users will need to update the Cardano node dependencies. The protocol would remain unchanged,
   same with client code. 
3. If we add new operations, deprecate old ones, or change the semantics of our protocol events, we would need all users to update their nodes in 
   order to keep a consistent view of DIDs and credentials state.

Note that the 3 scenarios illustrate different consequences. In the first case, if the PRISM node operator does not upgrade its node, then it will 
still be able to follow the protocol and keep a consistent view with the rest of the PRISM nodes. In the second scenario, if the user does not update
its node, he won't be able to continue reading new events (and may have problems sending new events too), because the Cardano node won't be able to 
keep in sync with the network. Therefore, the user will be behind other nodes. In the third case, we have something that could be similar to case 2,
the user will be out of sync with respect to updated nodes. However, the user will see old (possibly deprecated) events as valid, creating additional
security risks.
We could remark that it seems reasonable to conclude that all nodes running the same version will always see the same state.


How could we mitigate the risks of cases 2 and 3? 
A priori, we could rely on external coordination to make updates. However, there may be actors that may not follow as carefully as they should the 
update news.


## Proposal

The proposal we offer is to add an special protocol event, `VersionUpdate`. The event should be signed by a special key (or DID) controlled by IOHK.
The idea is that every node will have their own version hardcoded (or grabbed from config files). The node will process events normally and whenever 
it sees a `VersionUpdate` event, it will retrieve the new version from the message and compare against its own internal version. If the node version
is behind and incompatible with the event version, then it will stop processing further events. We could also force the node to not send new events
to the blockchain when outdated.
An example

```proto
message VersionUpdate {
  VersionInfo version = 1; // information of the new version
  string signing_key_id = 2; // id of the IOHK key used to sign this event
  bytes signature = 3; // signature on the `version` field using IOHK's key
}

message VersionInfo {
  string new_version = 1; // new version to be announced, we can use semantic versioning
  string effective_since = 2; // optional Cardano block number
}
```

### Comments on backward compatibility

It is interesting to consider that we have different alternatives to manage code complexity as we upgrade the system.
On one hand, we could support all the legacy events and semantics of previous versions. On the other hand, given that we are marking on-chain the 
update epoch, this implicitly defines a snapshot of our system. Meaning that we can distribute a new version of the software that at some point 
replaces legacy code by a snapshot of the system state. This would be a verifiable snapshot, and could help faster bootstrap in the future.

