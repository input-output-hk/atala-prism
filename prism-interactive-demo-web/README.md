# Prism interactive demo Web

## Running the project

### Install NodeJs and NVM

#### Mac

```bash
brew install nvm
```
#### Linux & Windows

Download and install [nvm](https://github.com/nvm-sh/nvm) for your operating system from github

To ensure the nodejs version is standardized, run

```bash
nvm use
```
which will set the node version to 10.16.3, if installed, otherwise install it via nvm and then run the command again.

Prior to running the app install the dependencies with `npm install`.


Lastly, to run the project use `npm run start` or just `npm start`.
To run the project with dev environment applied, run `npm run dev`

### Install protoc and grpc-web

#### Mac
protoc

```bash
brew install protobuf
```
grpc-web
```bash
brew install protoc-gen-grpc-web
```

#### Linux & Windows

Download [protoc](https://github.com/protocolbuffers/protobuf/releases) and [grpc-web](https://github.com/grpc/grpc-web/releases) for your operation system and add them to `PATH`.

Tested versions of this libraries are specified in [CircleCI config](../.circleci/config.yml)

#### Compiling proto files

```bash
npm run compile-protos
```
This will compile all proto files and put them in `src/protos/intdemo` directory.

*NOTE:* Usually you won't have to do it manually, because this script runs every time you run the app via `npm start` or `npm run dev`

### Install Envoy

[Envoy](https://www.envoyproxy.io/) is a proxy server through which the fronted communicates with the backend, Envoy is required to proxy the grpc-web calls to the backend services.

#### Linux

Run envoy on linux in docker

```bash
docker run --rm -ti --net=host -v $PWD/envoy/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.16-latest
```

#### Mac

On MacOs you can not run envoy in docker, because `--net=host` does not work, envoy won't be able to communicate with the backend because it will be running on the different network inside the docker container. In this case you can install envoy on your mac and run it this way.

Install

```bash
brew install envoy
```

Run

```bash
envoy -c $PWD/envoy/envoy.yaml
```
*NOTE:* There is a bug in envoy on mac, sometimes the DNS resolution does not work, so it can not translate `localhost` to `127.0.0.1` described in `.yaml` config file, see the [stackOverflow post](https://stackoverflow.com/questions/66910297/envoy-assert-failure-interface-index-0).


## Backend

you need to have `connector` running, for instructions on how to do it, see [running the backend](../prism-backend/README.md#run-services)

### Docker build

1. Environment variables in `.env` config file are used as default.
1. Build deployment bundle with `npm run build`
1. Build docker image `docker build .`.
1. Run container `docker run -it -p 80:HOST_PORT -e REACT_APP_GRPC_CLIENT=http://localhost:10000 --rm <image>`.
