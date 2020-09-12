## Running

Start the database first:

```sh
docker run -it --rm -e POSTGRES_DB=atala_mirror_db -p 5432:5432 postgres:11.5
```

Then run Mirror GRPC server:

```sh
mill -i mirror.run
```

## Usage

Example request:

```
grpcurl -import-path protos/ -proto protos/mirror_api.proto \
  -plaintext localhost:50057 \
  io.iohk.atala.mirror.protos.MirrorService/CreateAccount
```
