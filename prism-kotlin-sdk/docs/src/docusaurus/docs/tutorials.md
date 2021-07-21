---
sidebar_position: 1
id: tutorials
slug: /
---
# Before You Start
You are encouraged to follow the [Usage Tutorial](usage-tutorial/basics) **before** working on any integration, once you are ready to integrate **Atala PRISM**, checkout the [Integration Tutorial](integration-tutorial/introduction).

Our server-side APIs are powered by [gRPC](https://grpc.io), which allows you to take our [Protocol Buffers (protobuf)](https://developers.google.com/protocol-buffers/) definitions and generate the necessary API client for the most popular programming languages.

Make sure to check our [gRPC API reference](/protodocs/common_models.proto) for further information.

The current version of SDK **supports** both interaction with [Cardano](https://cardano.org/) blockchain and with in-memory ledger. All interactions with **Cardano** take more time so using in-memory ledger is preferred in this tutorial.
