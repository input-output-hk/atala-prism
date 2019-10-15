# Credential Verification iOS

## gRPC PoC

### Running the server

We use [nvm](https://github.com/nvm-sh/nvm) to handle node versions.

Prior to running the server, install the dependencies with `npm install`.

Lastly, to run the server use `node simple_proto_server.js`.

### Running the app

For now, the iOS app has a single screen that connects to the server sending a *Token* and receiving a *Issuer*, both are now hardcode, but with differents counters, so the client-server layers are easy to notice.
