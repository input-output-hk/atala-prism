# Building and running PRISM via docker compose

## 1. Build java assemblies for the components

To build and run the PRISM docker image per component one needs to follow these steps:

1. `cd prism-backend`
2. `sbt node/docker && sbt connector/docker`

Note, the steps above do not build an up to date copy of the `prism-management-console-web` front-end. It is usually
better to run the backend components using docker-compose then run the front-end with npm configuring it to use
the docker backend (how to do that is described in the `prism-management-console-web` project itself).

## Run compose

1. Run ```./compose.sh```

## Adding new components

If one needs to add new components to PRISM please add it to the docker-compose.yml file.
