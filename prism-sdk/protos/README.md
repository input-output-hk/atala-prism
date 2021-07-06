# protos
This is the source of truth for all the protobuf models and APIs.

We assume that every model can be used by other applications but the files ending with `_internal`, which are used only for the internal API and shouldn't be included outside of the backend.

The files ending with `_api` are specifying the public APIs, which depend on other files.

## Documentation
This section has guidelines on how to write/maintain PRISM protobuf documentation. Generally speaking, this guide follows Javadoc style guide with some protobuf-specific tweaks.

First let's introduce some terminology:
* A **leading comment** is a multi-line comment (enclosed in `/** ... */`) that is written before the relevant declaration.
* A **trailing comment** is a single-line comment (prepended by `//`) that is written after the relevant declaration on the same line.

### Service
* Protobuf service documentation must be written as a leading comment.
* Documentation must include what API the service represents and a brief overview of what the service does.

Example:
```proto
/**
 * Service for PRISM Vault API. Provides a way to store and retrieve
 * encrypted payloads.
 */
service EncryptedDataVaultService {
    
}
```

### RPC

* RPC documentation must be written as a leading comment.
* Documentation must start with one of the words describing authorization prerequisites for this RPC: `PUBLIC`, `AUTHENTICATED`. Leave an empty line after the word.
* Documentation must include a description of what this RPC does as well as provide any relevant context for calling this method (e.g. documentation should mention if this method should only be called after calling some other method or if it is paginated).
* Unlike with Javadocs, there is no need to document general request message fields and/or response message fields. Those will be described by the relevant message documentation. One exception is call-specific non-model parameters that should be mentioned in the RPC documentation (e.g. whether RPC allows specifying sorting order or filtering by a certain field).
* If RPC can fail with errors, leave a blank line at the end of the comment and add an `Errors:` section where you list [all possible errors](https://grpc.github.io/grpc/core/md_doc_statuscodes.html) in the following format: `- $DESCRIPTION ($GRPC_ERROR_CODE)`. If your RPC does not fail with errors, do not add the error section to documentation.

Examples:
```proto
/**
 * AUTHENTICATED
 * 
 * Sends multiple messages atomically. If one message cannot be sent (because
 * of an unknown connection, for example), the rest also remain unsent. This
 * method uses connection tokens instead of connection IDs.
 *
 * Errors:
 * - Unknown connections (UNKNOWN)
 * - Connections closed (FAILED_PRECONDITION)
 */
rpc SendMessages (SendMessagesRequest) returns (SendMessagesResponse) {}
```

```proto
/**
 * AUTHENTICATED
 *
 * Retrieves the available contacts for the authenticated institution. Several
 * filter and sorting options are available for finding specific contacts
 * (see GetContactsRequest documentation for more details).
 */
rpc GetContacts (GetContactsRequest) returns (GetContactsResponse) {}
```
### Message
* Protobuf message documentation must be written as a leading comment.
* If your message represents a request/response, use the following name convention: `<RPC method name>Request`, `<RPC method name>Response`.
* Documentation must provide extensive description for the context of using this message. If a message is a request/response for a specific RPC this should be clearly stated in the description along with the relevant RPC names. If your message represents some domain model this should also be mentioned.

Example:
```proto
/**
 * Represents a request that can be used to store data for the authenticated
 * user. See EncryptedDataVaultService.StoreData for more information.
 */
message StoreDataRequest {
}
```

### Message field
* Documentation for a message field can be written either as a leading comment or as a trailing comment. However, do not mix these styles in a single message; if the description for a field becomes too complex consider switching to leading comments for all message fields. However, if you use leading comments **do not** leave blank lines otherwise the documentation output will be mangled. Also, make sure to use dots (`.`) for ending sentences as new lines are being simply removed in the output.
* Documentation must provide the purpose of this field in respect of the containing message as well as any additional format requirements outside of the field type (e.g. a number belonging to a certain interval, a string conforming to a regexp or a list being of a certain size).
* All fields are non-optional by default. If a field is optional, specify so and what it would mean if it was missing.
* If a message has any internal messages and/or enums, you can presume that their documentation hold the context from the outer message and there is no need to repeat it.

Example:
```proto
message StoreDataRequest {
    string external_id = 1; // Client-generated UUID; should be unique for semantically different purposes.
    bytes payload_hash = 2; // SHA256 hash of the payload content (must have 32 bytes).
    bytes payload = 3; // The content of the payload encrypted by the user.
}
```

### Enum
* Protobuf enum documentation must be written as a leading comment.
* Similarly to message fields, enum values can be written either as a leading comment or as a trailing comment. However, do not mix these styles in a single enum; if the description for an enum value becomes too complex consider switching to leading comments for all enum values.
* If enum represents a domain model with fixed number of values (e.g. `LedgerType`, `BlockStatus`), provide description of such model in the documentation. If enum represents something more abstract (e.g. `SortingDirection`, `FilterType`), the purpose of such abstraction should be stated in the documentation. Document all enum values accordingly as well.
* Try to avoid giving enum values names that can be interpreted in multiple ways. For example, if you are trying to represent connection status, prefix all enum values with `STATUS_`. This way it will be clear that `STATUS_CONNECTION_MISSING` represents an entity's status where it is missing connection, whereas `CONNECTION_MISSING` can be easily mistaken for an error type.

Example:
```proto
/**
 * Represents the direction in which to sort search results. Used
 * in various requests to specify the format of desired response.
 */
enum SortByDirection {
    SORT_BY_DIRECTION_UNKNOWN = 0; // Nothing provided, each API can define whether to fail or take a default value (commonly ASCENDING).
    SORT_BY_DIRECTION_ASCENDING = 1; // Sort the results in ascending order.
    SORT_BY_DIRECTION_DESCENDING = 2; // Sort the results in descending order.
}
```

### Reserved declarations

Protobuf `reserved` declarations must be written at the top of a message definition.

Example:
```proto
message TimestampInfo {
    reserved 1; // Removed blockTimestamp_deprecated field
    reserved "blockTimestamp_deprecated";

    uint32 block_sequence_number = 2; // The transaction index inside the underlying block.
    uint32 operation_sequence_number = 3; // The operation index inside the AtalaBlock.
    google.protobuf.Timestamp block_timestamp = 4; // The timestamp provided from the underlying blockchain.
}
```

## Notes
- It looks like we need to keep all files on the same directory and they should belong to the same package, otherwise, the compilation fails.
- `protoc` seems to fail when compiling to JavaScript when there are messages/methods with the same name, even if they are inside a namespace, for example, `service a {}` and `service b {}` can't have `rpc doGet ...` because the compiler fails.

## Naming and formatting
Please follow [the google style guide](https://developers.google.com/protocol-buffers/docs/style) for proto files.