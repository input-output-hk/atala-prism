# Indy PoC
The project contains proof of concepts using the Indy SDK.

Most examples are based on the official [Indy how-to guides](https://github.com/hyperledger/indy-sdk/tree/master/docs/how-tos).

## How to run
- Compile libindy
- Register the libindy library to the library search path, there are some ways:
  * Add the path to the `LD_LIBRARY_PATH`, like `/home/dell/projects/indy/indy-sdk/libindy/target/debug`
  * Copy `libindy.so` to `/usr/local/bin`
- Build the indy pool, clone the indy-sdk and move to that folder, then, `docker build -f ci/indy-pool.dockerfile -t indy_pool .`
- Run the indy pool, `docker run -it -p 9701-9708:9701-9708 indy_pool`
- Run the app, `mill -i indy-poc.run`
- Choose any example.

## Trouble-shooting
- On `NullPointerException`, be sure that the native libindy is registered properly, it's common to get this error while running the app with IntelliJ, try the console instead.
- On runtime exceptions, try cleaning the indy data: `rm -rf ~/.indy_client`
- On the app hanging forever while choosing the example to run, ensure mill runs on interactive mode (`-i` option): `mill -i indy-poc.run`
