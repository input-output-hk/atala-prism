# protos
This is the source of truth for all the protobuf models and APIs.

We assume that every model can be used by other applications but the files ending with `_internal`, which are used only for the internal API and shouldn't be included outside of the backend.

The files ending with `_api` are specifying the public APIs, which depend on other files.

## Notes
- It looks like we need to keep all files on the same directory and they should belong to the same package, otherwise, the compilation fails.
- `protoc` seems to fail when compiling to JavaScript when there are messages/methods with the same name, even if they are inside a namespace, for example, `service a {}` and `service b {}` can't have `rpc doGet ...` because the compiler fails.
