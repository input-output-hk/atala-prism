## Running

Start the database first:

```sh
docker run -it --rm -e POSTGRES_DB=atala_mirror_db -p 5434:5432 postgres:11.5
```

To create example DID, run:

```sh
sbt "connectorClient/run register"
```

then update Mirror's configuration (in application.conf or env):

```sh
export ATALA_MIRROR_CONNECTOR_DID="<did form the above command>"
export ATALA_MIRROR_CONNECTOR_DID_PRIVATE_KEY="<private key form the above command>"
```

Then run Mirror GRPC server:

```sh
ATALA_MIRROR_PSQL_HOST=localhost:5434 sbt mirror/run
```

## Usage

Example request:

```
grpcurl -import-path ../prism-sdk -proto protos/mirror_api.proto \
  -plaintext localhost:50057 \
  io.iohk.atala.mirror.protos.MirrorService/CreateAccount
```

## E2E testing

### Prerequisites

* install development version of the Atala PRISM mobile app (I’ve used iOS version via TestFlight and Android Emulator)
* install `libqrencode` for QR code generation (not required, but allows to create QR  code from the shell)
* create DID (I’ve created one with `sbt "connectorClient/run register -h develop.atalaprism.io -p 50051 -u c8834532-eade-11e9-a88d-d8f2ca059830"`)
* go through the demo at https://develop.atalaprism.io/credentials to obtain credentials

### Steps

1. Configure Mirror with develop connector:
```sh
export ATALA_MIRROR_CONNECTOR_HOST="develop.atalaprism.io"
export ATALA_MIRROR_CONNECTOR_PORT=50051
export ATALA_MIRROR_CONNECTOR_DID="did:prism:635cd53a076b05cbb6d880e6c3860357927dfaef445fe196a502ae9c257d50b6"
export ATALA_MIRROR_CONNECTOR_DID_PRIVATE_KEY="BSbQDmDAukupfCpWSqDwsPaPrQWG3tV5V742It-892A="
```

2. Start Mirror:
```sh
docker run -it --rm -e POSTGRES_DB=atala_mirror_db -p 5434:5432 postgres:11.5
ATALA_MIRROR_PSQL_HOST=localhost:5434 sbt mirror/run
```

3. Create QR code form new connection token:
```sh
grpcurl -import-path ../prism-sdk -proto protos/mirror_api.proto \
  -plaintext localhost:50057 \
  io.iohk.atala.mirror.protos.MirrorService/CreateAccount | \
jq -r '.connectionToken'| tr -d '\n\t' | qrencode -t UTF8
```

4. Scan the QR code with the mobile app.

5. Wait for credential request on the mobile app (it can take a few minutes).
