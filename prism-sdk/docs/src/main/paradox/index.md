@@@ index
* @ref:[Usage Tutorial](usage-tutorial/index.md)
* @ref:[Modules](modules/index.md)
* @ref:[Grpc API reference](grpc-api.md)
@@@

# Atala PRISM SDK
This is the official Software Development Kit (SDK) for the Atala PRISM project.

You are encouraged to follow the @ref:[Usage Tutorial](usage-tutorial/index.md) *before* working on any integration.

Our server-side APIs are powered by [gRPC](https://grpc.io), which allows you to take our [Protocol Buffers (protobuf)](https://developers.google.com/protocol-buffers/) definitions and generate the necessary API client for the most popular programming languages.
 
Make sure to check our @ref:[gRPC API reference](grpc-api.md) for further information.

## SDK Modules
The SDK modules provide all the necessary functionality to deal with cryptographic primitives, identities, and credentials.

* The @ref:[Crypto](modules/crypto.md) module provides the necessary cryptographic primitives required mainly by other components.
* The @ref:[Identity](modules/identity.md) module includes information to work with Decentralized Identifiers (DIDs). We are in the process of becoming [DID Spec-compliant](https://w3c-ccg.github.io/did-spec/).
* The @ref:[Credentials](modules/credentials.md) module provides information to work with Verifiable Credentials (VCs). We are in the process of becoming compliant with the [Verifiable Credentials Data Model](https://w3c.github.io/vc-data-model/).
* The Protos module includes the necessary protobuf definitions required to interact with our server-side API. This module also includes the models required while encoding data to its binary representation.
* The @ref:[Connector](modules/connector.md) details utility functions to simplify the interaction with server-side APIs (e.g., APIs required to authenticate requests).
