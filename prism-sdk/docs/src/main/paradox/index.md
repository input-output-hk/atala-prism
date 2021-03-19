@@@ index
* @ref:[Usage Tutorial](usage-tutorial/index.md)
* @ref:[Modules](modules/index.md)
* @ref:[Grpc API reference](grpc-api.md)
@@@

# PRISM SDK
This is the official SDK for the Atala PRISM project.

You are encouraged to follow the @ref:[Usage Tutorial](usage-tutorial/index.md) before working on any integration.

Our server-side APIs are powered by [grpc](https://grpc.io), which allows you to take our [protobuf](https://developers.google.com/protocol-buffers/) definitions, and generate the necessary API client for the most popular programming languages.
 
Make sure to check our @ref:[Grpc API reference](grpc-api.md).

## Modules
The SDK modules provide all the necessary functionality to deal with cryptographic primitives, identities, and credentials.

* The @ref:[Crypto](modules/crypto.md) module provides the necessary cryptographic primitives, mainly required by other components.
* The @ref:[Identity](modules/identity.md) module provides the necessary stuff to work with Decentralized Identifiers. We are in the process of becoming compliant with the [DID Spec](https://w3c-ccg.github.io/did-spec/).
* The @ref:[Credentials](modules/credentials.md) module provides the necessary stuff to work with Verifiable Credentials. We are in the process of becoming compliant with the [Verifiable Credentials Data Model](https://w3c.github.io/vc-data-model/).
* The Protos module includes the necessary protobuf definitions to interact with our server side API, as well as models required while encoding data to its binary representation.
* The @ref:[Connector](modules/connector.md) module provides utility functions to simplify the interaction with the server side APIs, like the necessary APIs to authenticate requests.
