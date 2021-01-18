# PRISM SDK
This is the official SDK for the Atala PRISM project.

You are encouraged to follow the [Usage Tutorial](wiki/basic-usage) before working on any integration.

While the SDK is designed to perform most operations locally, some functions require data retrieved from the server-side APIs, make sure to check the [protos](wiki/modules/protos) to understand how to invoke those.

Our server-side APIs are powered by [grpc](https://grpc.io), which allows you to take our [protobuf](https://developers.google.com/protocol-buffers/) definitions, and generate the necessary API client for the most popular programming languages.

Make sure to check our [Grpc API reference](grpc-api).

## Modules
The SDK modules provide all the necessary functionality to deal with cryptographic primitives, identities, and credentials.

* The [Crypto](wiki/modules/crypto) module provides the necessary cryptographic primitives, mainly required by other components.
* The [Identity](wiki/modules/identity) module provides the necessary stuff to work with Decentralized Identifiers. We are in the process of becoming compliant with the [DID Spec](https://w3c-ccg.github.io/did-spec/).
* The [Credentials](wiki/modules/credentials) module provides the necessary stuff to work with Verifiable Credentials. We are in the process of becoming compliant with the [Verifiable Credentials Data Model](https://w3c.github.io/vc-data-model/).
* The [Protos](wiki/modules/protos) module includes the necessary protobuf definitions to interact with our server side API, as well as models required while encoding data to its binary representation.
* The [Connector](wiki/modules/connector) module provides utility functions to simplify the interaction with the server side APIs, like the necessary APIs to authenticate requests.
