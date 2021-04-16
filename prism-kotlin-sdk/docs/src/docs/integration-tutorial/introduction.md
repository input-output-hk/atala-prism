# Integration Tutorial

This tutorial walks you through the necessary steps to integrate the Atala PRISM SDK in your project.

By the end of the tutorial, you should have learned all the necessary pieces to build an application like our [interactive demo](https://atalaprism.io).

The provided examples is written in [Kotlin](https://kotlinlang.org/), which can be easily migrated to other programming languages targetting the Java-Virtual-Machine (JVM).

The purpose of these examples is to be verbose with the hope that you find them simple enough to create your own abstractions.


## Actors
There are 3 actors involved in the code:
1. Issuer: Acts as an institution that issues credentials.
2. Verifier: Acts as an institution that receives and verifies credentials.
3. Holder: Acts as a person, who wants to get a credential from the issuer, so that it gets validated by Verifier.


## Services
There are 2 backend services involved in the examples:
1. Connector: Provides a way to open a communication channel between two entities, which allows Issuer to send a credential to Holder.
2. Node: Implements the Atala PRISM Slayer protocol, which is what gets the proofs about DIDs/Credentials published on Cardano ledger, as well as allows to query such data.
