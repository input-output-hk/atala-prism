## Running

Start the database first:

```sh
docker run -it --rm -e POSTGRES_DB=atala_mirror_db -p 5434:5432 postgres:11.5
```

To create example DID, run:

```sh
mill -i connector.client.run register
```

then update Mirror's configuration (in application.conf or env):

```sh
export ATALA_MIRROR_CONNECTOR_DID="<did form the above command>"
export ATALA_MIRROR_CONNECTOR_DID_PRIVATE_KEY="<private key form the above command>"
```

Then run Mirror GRPC server:

```sh
ATALA_MIRROR_PSQL_HOST=localhost:5434 mill -i mirror.run
```

## Usage

Example request:

```
grpcurl -import-path protos/ -proto protos/mirror_api.proto \
  -plaintext localhost:50057 \
  io.iohk.atala.mirror.protos.MirrorService/CreateAccount
```
