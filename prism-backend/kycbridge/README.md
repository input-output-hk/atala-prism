# KYC Bridge

KYC Bridge is a component bridging between PRISM ecosystem and remote identity verification services. It forwards the data provided by the user to the external service (currently only Acuant is supported) and if verification response is succesful it issues a credential with the obtained data.

The most obvious way to interact with it is using PRISM Wallet - either Android or iOS one. Communication between the wallet and KYC Bridge is a little bit hacky: the wallet first accesses GRPC endpoint defined in `prism-sdk/protos-src/kycbridge_api.proto` at port `8081` of the same host as Connector to obtain connection token and then communicates via PRISM Connector messages.

## KYC Bridge in AWS

If you want to just play with KYC Bridge, the easiest approach is to use instance deployed in AWS from `develop` branch. PRISM wallet will connect to it by default.

## Running KYC Bridge locally

Process of running KYC Bridge locally is quite complex:

1. You need to run PRISM Node and PRISM Connector locally

```sh
docker run -it --rm -e POSTGRES_DB=node_db -p 5432:5432 postgres:11.5
sbt node/run
```

```sh
docker run -it --rm -e POSTGRES_DB=connector_db -p 5433:5432 postgres:11.5
CONNECTOR_PSQL_HOST=localhost:5433 sbt connector/run
```

2. Start the database:

```sh
docker run -it --rm -e POSTGRES_DB=kyc_bridge_db -p 5435:5432 postgres:11.5
```

3. Ask your colleagues at work for credentials for Acuant services. Then run:

```sh
export KYC_BRIDGE_ACUANT_USERNAME=username
export KYC_BRIDGE_ACUANT_PASSWORD=password
export KYC_BRIDGE_ACUANT_SUBSCRIPTION_ID=subscriptionId
export KYC_BRIDGE_IDENTITYMIND_USERNAME=username
export KYC_BRIDGE_IDENTITYMIND_PASSWORD=password
```

4. Then run KYC Bridge server:

```sh
KYC_BRIDGE_GRPC_PORT=8081 KYC_BRIDGE_PSQL_HOST="localhost:5435" sbt kycbridge/run
```

5. You can start using PRISM wallet:
 - In case of Android first skip identity verification, configure backend (both Connector and KYC Bridge need to be available on network)
