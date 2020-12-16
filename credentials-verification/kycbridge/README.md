## Running

Node and connector must be running:

To start node:

```
docker run -it --rm -e POSTGRES_DB=node_db -p 5432:5432 postgres:11.5
sbt node/run
```

To start connector:

```
docker run -it --rm -e POSTGRES_DB=connector_db -p 5433:5432 postgres:11.5
CONNECTOR_PSQL_HOST=localhost:5433 sbt connector/run
```

Start the database:

```sh
docker run -it --rm -e POSTGRES_DB=kyc_bridge_db -p 5435:5432 postgres:11.5
```

To create example DID, run:

```sh
sbt "connectorClient/run register"
```

then update KycBridge configuration (in application.conf or env):

```sh
export KYC_BRIDGE_CONNECTOR_DID="<did form the above command>"
export KYC_BRIDGE_CONNECTOR_DID_PRIVATE_KEY="<private key form the above command>"
```

Ask your colleagues at work for `username`, `password` and `subscriptionId` of AssureId service.

Then run:

```
export KYC_BRIDGE_ACUANT_USERNAME=username    
export KYC_BRIDGE_ACUANT_PASSWORD=password
export KYC_BRIDGE_ACUANT_SUBSCRIPTION_ID=subscriptionId
```

Then run Kyc Bridge server:

```sh
sbt kycbridge/run
```

Generate connection token:

```
grpcurl -import-path ../prism-sdk -proto protos/kycbridge_api.proto \
  -plaintext localhost:50050 \
  io.iohk.atala.kycbridge.protos.KycBridgeService/CreateAccount
```