This tutorial walks a user through the necessary steps to integrate the **Atala PRISM SDK** in a project.

By the end of the tutorial, one should have learned all the necessary pieces to build an application like [Atala PRISM Interactive Demo Website](https://atalaprism.io).

Provided examples are written in [Kotlin](https://kotlinlang.org/), which can be easily migrated to other programming languages targeting the Java-Virtual-Machine (JVM).

The purpose of these examples is to be verbose with the hope that one will find them simple enough to create one's own abstractions.

## Actors
There are 3 actors involved in the code:
1. **Issuer** - acts as an institution that issues credentials.
2. **Verifier** - acts as an institution that receives and verifies credentials.
3. **Holder** - acts as a person, who wants to get a credential from **Issuer**, so that it gets validated by **Verifier**.

## Services
There are 2 backend services involved in the examples:
1. **Connector** - provides a way to open a communication channel between two entities, which allows `Issuer` to send a credential to `Holder`.
2. **Node** - implements the **Atala PRISM Slayer** protocol, which is what gets the proofs about **DIDs/Credentials** published on **Cardano** ledger, as well as allows to query such data.

**Note:** be aware that in this tutorial there is no real integration with [Cardano](https://cardano.org/), all components use in-memory ledger. Anyway the flow will be exactly the same once both **Node** and **Connector** interacts directly with **Cardano** ledger. For simplicity **Cardano** name is used as a default ledger in next steps.
