# PRISM Management Console Web 

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

#### Set local env file

copy `.env` file to `.env.local`

```bash
mv .env .env.local
```

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
./scripts/compile-protos.sh
```
This will compile all proto files and put them in `src/protos` directory.

*NOTE:* Usually you won't have to do it manually, because this script runs every time you run the app via `npm run`

### Install Envoy

[Envoy](https://www.envoyproxy.io/) is a proxy server through which the fronted communicates with the backend, Envoy is required to proxy the grpc-web calls to the backend services.

#### Linux

Run envoy on linux in docker

```bash
docker run --rm -ti --net=host -v $PWD/envoy/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.16-latest
```
If you interact with the console backend

```bash
docker run --rm -ti --net=host -v $PWD/envoy/envoy-console.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.16-latest
```

#### Mac

On MacOs you can not run envoy in docker, because `--net=host` does not work, envoy won't be able to communicate with the backend because it will be running on the different network inside the docker container. In this case you can install envoy on your mac and run it this way.

Install

```bash
brew install envoy
```

Run

```bash

envoy -c $PWD/envoy/envoy.yaml # use envoy-console.yaml if you interact with console backend
```
*NOTE:* There is a bug in envoy on mac, sometimes the DNS resolution does not work, so it can not translate `localhost` to `127.0.0.1` described in `.yaml` config file, see the [stackOverflow post](https://stackoverflow.com/questions/66910297/envoy-assert-failure-interface-index-0).

If needed, configure envoy proxy "listening port" at `.env.local` file.
For more information about environment variables see [Configurations](#Configurations)


### Start the server

First install npm dependencies 

```bash
npm install
```

Start the server

```bash
npm start
```
The management console will be available on http://localhost:3000/


## Configurations

react-scripts package internally uses `.dotenv`. For more information see [adding-custom-environment-variables](https://create-react-app.dev/docs/adding-custom-environment-variables).

Environment variables are configured in `.env` file.
For development purposes copy `.env` file to `.env.local`. This file is excluded from git and overrides `.env` for all environments except test.

### Docker build

These instructions are useful for our deploys, they may be outdated. They are not needed to run the app locally.

1. Environment variables in `.env` config file are used as default.
1. Build deployment bundle with `npm run build`
1. Build docker image `docker build .`.
1. Run container `docker run -it -p 80:HOST_PORT -e REACT_APP_GRPC_CLIENT=http://localhost:10000 --rm <image>`.

## Developer considerations

You should avoid destructuring or `...` operator with Api object in components. As I noticed, when using such features combined with `function` and `prototype` it produces to loose "this" context. 

In some components, you should also find some functions wrapped in other functions that look as unnecessary. This is a hack to preserve "this" context.

## scripts

- [compile-protos.sh](./scripts/compile-protos.sh): Compile the js bindings for the gRPC API.
- [nukestate.sh](./scripts/nukestate.sh): Used to reset the application state (to start over with a new database). This does not reset the browser's state, you must do that by deleting all the entries in its localstorage.
To run it, you must shut down the node, wallet and connector first.
*warning*: It'll `stop` and `rm` any container for which `docker ps` matches `\bpostgres\b`


### Running the whole project

See our main [README](../README.md#How-to-run) for instructions on how to run the whole project, including the backend and the chrome extension (which is necessary to log into the management console).

