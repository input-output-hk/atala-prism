## Running
In order to run the connector, you must first run the node.

## Connector client

The connector comes with simple CLI client, which right now is used for testing complex features manually.

### Register DID

```
mill -i connector.client.run register
```

This operation will prepare a request to publish a DID to the node, which is used as an argument while registering on the connector.
