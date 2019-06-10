# Atala + OBFT Technical design

<b>** ATTENTION **</b> This is live document, that will be kept changing with the code changes

## Intro

This is a document trying to describe how we have implemented OuroborosBFT, and a framework around it (Atala)

## Approach and Challenges

### Premises

What Atala will be in the end is very much in the air, and as such the current implementation (located in the `atala` folder of this repo) has been designed with some premises in mind. All this might very well end up being incorrect, but right now they allow us to start with the implementation and design. Here they are:

* Atala is going to be distributed in the form of a library or group of libraries. Given the scope of the library/ies, and the central place we envision these libraries are going to have in the applications developed with them, we are going to use the word 'framework' (instead of library) more often than not
* The applications developed with Atala are going to be based around (or at least require) some form of distributed data storage
* This distributed data can't be replaced by some already provided solution in the lines of an oracle database. Either because the application being developed is intended to replace such a solution or, more probably, because it needs to be distributed around the public internet or provide some sort of 'peer to peer' aspect

### Goals
* Provide a working implementation of OuroborosBFT, as described [in the paper](https://eprint.iacr.org/2018/1049.pdf)

## Initial progress
### Proof of concept

In the Atala repository exists a sample application, with a simple CLI that emulates a cluster of nodes implemented using Atala (mainly it's OBFT implementation). This application emulates a very simple in-memory key-value store, agreed upon between the nodes using OBFT

With `bazel` installed and from within a local copy of the Atala repository, you can run this sample application with this command

```bash
bazel run //atala/apps:AppPoC
```

### Identity Ledger testnet (Work in Progress)

We are setting up a testnet on AWS that will contain an Identity Ledger application which we are also currently developing

## Current Design

### High Level view

#### Definitions

**Atala**: a framework/library used to develop (certain types of) distributed applications

**The Application**: An application developed using Atala

**The Application cluster**: The group of nodes that run the application

**The Consensus cluster**: A group of nodes that agree on certain state, as described in the [consensus algorithm abstraction](#consensus-algorithm-abstraction). This nodes are typically going to be the same nodes as the ones that form the application cluster

**The Consensus algorithm**: The algorithm used by the nodes in the consensus cluster to achieve data consensuation

#### Personas

**Application developer**: The people developing/maintaining/running the application

**End user**: End users of the application

#### Types of nodes

**Server**(or '*peer*'): The nodes that run the application (including the consensus algorithm)

**Client** The software run by the end user to communicate with the server

### Consensus algorithm abstraction

While working on the implementation of OBFT, we realised that, once stripped to the bare minimum, what a consensus algorithm needs to do is to achieve consensus between nodes on the contents of a List of values. It's important to note here that these values are not blocks, blocks are an internal implementation detail of the consensus algorithm and completely irrelevant to the component above that uses the consensus algorithm.

#### Simplified concept of 'consensus'

This simplified concept of `consensus`, is characterised by this ideas:

- It's an agreed upon list of values
- These values are called 'transactions', but are just values (although for the Application developer, this values will usually represent write operations used to compute some sort of state)
- The consensus provides one (public) operation and one mechanism to access the data on the list:
  * AppendTransaction operation: 
  * View: a Monix stream with all the transactions as they are regarded as `finalised`
- Only two guarantees are offered:
  1) Finalised: Once a value is stable in the list, it's guaranteed to remain in the list (in OBFT this guarantee seems to not be true in the case of prolonged network partitions)
  2) Sorted: Once a value is stable in the list, it will remain in the same relative position within the list
      - as before, in OBFT network partitions can be a problem
      - the order of the list can (and in OBFT probably will) be different form the order in which the values have been requested to be added in the list
- Notably *NOT* guaranteed:
  * Delivery: If a value has been requested to be added into the list, it doesn't necessarily mean it will end up in there. In OBFT delivery is the standard behaviour but, as stated, is not guaranteed
  * Uniqueness: There is no guarantee that a value is going to appear only once in the list. ~~In fact, in OBFT most of values will appear more than once in the list~~ In OBFT values will appear usually once, but this is not garanteed

### Organisation

#### Dependency injection

The codebase uses a simple constructor-based dependency injection mechanism:
- All the dependencies of a component are passed to that component using the main constructor
- The apply method of the companion object of the component will accept some parameters and generate others internally when it makes sense. It is, in fact, in the apply method that the actual 'dependency injection configuration' happens

This pattern is simple, clear, based on the Scala language itself and doesn't relay on ugly patterns like the 'cake pattern'

#### Packages

Currently, beyond a couple of support elements, Atala consists, mainly, of the implementation of OuroborosBFT and the network component used to integrate Atala (currently obft) with [scalanet](https://github.com/input-output-hk/scalanet)

This is the current structure of the `obft` (short for Ouroboros BFT) package in Atala:

```
  atala
  ├──── network
  ├──── config
  ╰─┬── obft
    ├──── common
    ├─┬── blockchain
    │ ├──── models
    │ ╰──── storage
    ╰──── mempool
```

**common:** common elements used across the different packages in obft (currently only containing the `TransactionSnapshot` type)

**blockchain:** representation, storage and management of the blockchain used internally by obft

**mempool:** in-memory collector of transactions that collects the transactions that might become a block in the future

### Configuration

Atala can be configured using a HOCON file like this:

```hocon
server-index: 4 # The index of this node inside the cluster
public-key: "..."
private-key: "..."
database: "/path/to/file/containing/local/database"
remote-nodes: [
  {
   server-index: 1 # The index of that node inside the cluster
   public-key: "..."
   address: {host: "localhost", port: "8001"} # The address this node is listening on
  },
 
  ...

  {
   server-index: n
   public-key: "..."
   address: {host: "localhost", port: "8009"}
  }
]
time-slot-duration: 300 millis # The duration of a single obft time slot
address: {host: "localhost", port: "8004"} # The address this node is going to be bind to
```


### Initialisation

To initialise an instance of OuroborosBFT, the codebase should look similar to this:

```scala

  lazy val networkInterface: OBFTNetworkInterface[InetMultiAddress, Tx] = ???
  lazy val obftChannel = Await.result(networkInterface.networkChannel().runAsync, 10.seconds)
  
  // A monix stream that sends a tick message every time a time slot change happens
  val clockSignalsStream: Observable[Tick[Tx]] = {
    Observable
      .intervalWithFixedDelay(timeSlotDuration)
      .map { _ =>
        Tick(Clock.currentSlot(timeSlotDuration.toMillis))
      }
  }

  // Loading the configuration in this unsafe way, allows for a cleaner example
  val configuration = pureconfig.loadConfigOrThrow[ObftNode] 

  lazy val ouroborosBFT =
    OuroborosBFT[Tx](
      configuration.serverIndex, // The 1-based index of this node in the list of nodes of the `Consensus Cluster`
      firstTimeSlot,             // The first time slot this node will operate on,
                                 // usually Clock.currentSlot(configuration.timeSlotDuration.toMillis),
      configuration.keyPair,     // The keyPair used by this node
      configuration.genesisKeys, // Content of the genesis block
      clockSignalsStream,
      configuration.database     // File that is going to hold the storage database used by the blockchain
                                 // storage
    )

```

### Time

The time is managed in time slots, as described in the Ouroboros BFT paper. The whole implementation keeps track of the passage of time through a monix stream that ticks a message every time there is change of time slot.

Currently this stream is governed by the system clock, but being a stream injected into the main OuroborosBFT class, it will be easy to replace this stream by another one governed by a logical clock as described in the paper.

### APIs

We are working on publishing the Atala scaladocs, when we have done that, this will link to the different APIs in atala:

* Network
* OuroborosBFT
* Mempool
* Blockchain

### Testing

We are ensuring the quality of Atala with six layers of testing

**Developer manual testing:** As described in [proof of concept](#proof-of-concept), we have a console application that offers a CLI and an emulated Atala cluster, which is extensively used during development to manually ensure that the different features work as intended (and having already helped unearthing and solving more than one bug)

**Unit testing:** Each component in Atala has it's own complete suite of unit tests

**Internal integration testing:** we have created a suite of integration tests that test OBFT end-to-end against an internally emulated Atala cluster

**Formal manual testing:** We perform a suite of manual tests whenever some major milestones are achieved

And currently as a WiP:

**External e2e integration tests:** in a separate testing repository called atala-qa-automation we will perform integration tests treating atala/obft as a blackbox to make sure that our solution works as designed

**Performance tests:** we are writing a suite of performance tests that will help us discover and study how many TPS (transactions per second) OBFT can do in a) local mode (3 virtual nodes in a PC) and b) in a moderate AWS cluster. 

### Formal analysis

For a more formal description of the approach taken in our implementation, please check this confluence page:
https://input-output.atlassian.net/wiki/spaces/CE/pages/712409091/A-338+-+Ouroboros+BFT

### Technical Debt

- Move the clock component to be a component of obft
- UNSAFE VIEW: Currently OuroborosBFT has a `lazy val` `view` that connects directly to the main `actor` of OBFT. This means that:
    [YouTrack ticket](https://iohk.myjetbrains.com/youtrack/issue/A-867)
  * If somebody subscribes to the actor AND somebody else subscribes to the view, the messages of the actor are going to be processed twice
  * If there are n subscribers to the view, the messages of the actor are going to be processed n times
  * Since we need to allow people to subscribe to the view, currently there is nothing that subscribes to the actor. If the user forget (or don't wants) to subscribe to the view, OuroborosBFT is not going to start at all
  * This could be a solution: https://monix.io/docs/3x/reactive/observable.html#observablereplay-replaysubject

