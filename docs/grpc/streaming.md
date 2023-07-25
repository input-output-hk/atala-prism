# PRISM gRPC Streaming

This document describes a way to improve PRISM's handling of large gRPC messages. The work was done as part of analyzing MoE requirements, specifically bulk creation of contacts and credentials, but the proposed solution is not necessarily restricted to those particular RPCs.

## Context

Atala PRISM provides a way for the issuer to create a number of contacts and credentials atomically by providing a list of entities in a single RPC invocation. Although this seemed like a reasonable approach for the time, we realized that it is limited by gRPC protocol. By default, gRPC has a maximum message size of **4 MB** (as is [specified](https://grpc.github.io/grpc-java/javadoc/io/grpc/ManagedChannelBuilder.html#maxInboundMessageSize-int-) by ScalaPB's underlying grpc-java documentation). Although this setting is configurable, the protocol was not designed to handle large messages and should not be used in this fashion.

Given the new requirements where we may wish to create large amounts of data at once and the aforementioned limitation, a new system must be designed for handling such requests.

## Goal

We want to achieve a general-purpose solution that would:
- Retain the current API invariants (bulk data must be created atomically, a single response must be returned to user)
- Be able to handle arbitrary-sized bulk data (assuming reasonably uninterrupted internet connection)

## Proposal

We propose to use the built-in gRPC streaming capabilities. Right now most of the RPCs exposed by PRISM backend are [**unary RPCs**](https://grpc.io/docs/what-is-grpc/core-concepts/#unary-rpc). Trying to uphold this invariant is what led us into introducing specific bulk-creation RPCs (`CreateContacts`, `ShareCredentials` etc). For the purposes of this proposal, we are interested specifically in [**client streaming RPCs**](https://grpc.io/docs/what-is-grpc/core-concepts/#client-streaming-rpc), but the proposed solution can also easily support both server streaming RPCs and bidirectional streaming RPCs.

For every unary RPC `R1` that has a unary RPC counterpart `R2` that does the same as `R` but in bulk, we propose to replace them with a single client streaming RPC `R'`. The semantics of `R'` can be described as follows:
- Client sends a stream of `R1Request`-like messages, each one containing a single entity.
- Once client finishes, server responds with a single `R2Response`-like message that contains information/confirmation about all processed entities.
- If client cancels the RPC or loses connection midway, the RPC is considered to be [**cancelled**](https://grpc.io/docs/what-is-grpc/core-concepts/#cancelling-an-rpc). We guarantee the atomicity of the RPC: none of the data already received by the server will make it to the persistent PRISM state in such case. This means that the entire RPC must be completed in a single gRPC connection.

Note that it is easy to use `R'` as a substitute for both `R1` and `R2`.

### Implementation details

We propose to use [fs2-grpc](https://github.com/typelevel/fs2-grpc) a streaming gRPC library that is built on top of ScalaPB. Being a typelevel library and using ecosystem that we are already using (cats, cats-effects, fs2), it seems to be a reasonable choice for us.

Let's see what it will look like on a small scale. Consider existing RPCs:
```proto
rpc CreateContact (CreateContactRequest) returns (CreateContactResponse) {}
rpc CreateContacts (CreateContactsRequest) returns (CreateContactsResponse) {}
```

We replace them with a new RPC:
```proto
rpc CreateContact (stream CreateContactRequest) returns (CreateContactResponse) {}
```

The flow of the new RPC looks like this:
1. Server authenticates the request by the stream metadata (sent only once in the very beginning).
2. Server then validates all incoming stream messages.
3. The messages are batched in chunks (e.g. one chunk could have either 10 messages or fewer if under 10 messages arrived in the last 100 milliseconds).
4. The batches are then put into the temporary table (will be defined in more details later).
5. If a TCP error (client closed connection, timeout etc) occurs during the stream, server will close the connection from its end and purge the temporary table of the transient data.
6. If the stream half-closes from client's side successfully, then server will move all data from the transient tables to the real ones and will reply with a single response while closing the connection.

This is roughly what Scala code would look like with fs2-grpc:
```scala
override def createContact(
  requestStream: fs2.Stream[IO, console_api.CreateContactRequest],
  ctx: Metadata
): IO[console_api.CreateContactResponse] =
  auth(ctx) {
    requestStream
      .evalMap { request =>
        convertToModel(request)
      }
      .groupWithin(10, 100 millis)
      .evalTap { chunk =>
        repository.createContactsTemporarily(chunk)
      }
      .handleErrorWith { _ =>
        repository.purgeTemporaryState
      }
      .compile
      .drain
      .map { _ =>
        repository.persistTemporaryState
      }
  }
```

Some potential implementation pitfalls:
- Client-side streaming support in Kotlin Multiplatform might be tricky to implement. Especially for JavaScript and iOS.
- fs2-grpc does not seem to support generating grpcweb clients for Scala.js, so we will have to figure an alternative FP approach for the web-wallet in case we do not want to use the default `io.grpc.stub.StreamObserver`.

### Temporary tables
Consider PRISM table `T`. We will call table `T'` a temporary version of `T` if:
- `T'` is [unlogged](https://www.postgresql.org/docs/13/sql-createtable.html). Unlogged tables are much faster than the logged ones, the disadvantage is that it is very easy to lose recently written data in case of a crash. This is not an issues since we do not care about long-term persistence of data in the table.
- All columns from `T` are present in `T'`.
- None of the indices or constraints from `T` are present in `T'`.
- `T'` has a new random-generated UUID column `stream_id` that represents the stream this row is a part of. This column needs an index.
- `T'` has a new timestamp column `temporarily_recorded_at` that represents when the row was recorded in the database.

Note that `T'` does not have a primary key, but it still supports everything we need: copying and purging rows submitted in the same stream.

In case of a total system failure, we should also create a cron job that would occasionally sweep through `T'` and delete all old entries by using `temporarily_recorded_at`.

## Alternatives considered

### Akka gRPC
Pros:
- Being a more conventional library, Akka Streams API might be more familiar to the team
Cons:
- Unlike fs2 and Monix which are already being used in our project, we would be introducing yet another library for streaming/asynchronous programming
- Akka ecosystem tends to be oriented toward building distributed systems with Akka (especially actors). This is not our case and fs2, being a more lightweight library, suits us better. There is a good thread on Reddit that compares fs2 and Akka stack, especially [this](https://www.reddit.com/r/scala/comments/khvcam/can_someone_explain_the_tradeoffs_between_using/ggotmw5) message.

### io.grpc.stub.StreamObserver
Pros:
- Is available from ScalaPB by default, no need to add a third-party extension such as fs2-grpc
- Being used for some streaming services that we already have
Cons:
- The API provided by `StreamObsever` is designed to be used in a conventional Java application. It is not very nice when used from a FP application such as ours. Streaming **from server** using `StreamObserver` (i.e. what we currently do with it) is fine from architecture's point of view, but streaming to the server would be a pain to deal with.

### ZIO gRPC
We did not consider this option that deeply as no one in the team is familiar with ZIO, and we would rather avoid introducing yet another Scala ecosystem to the project.
