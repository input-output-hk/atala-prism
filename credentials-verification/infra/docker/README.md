# Building and running the CVP environment via docker compose

## 1. Build java assemblies for the components
To build and run CVP docker image per component one needs to follow these steps:
1. `cd credentials-verification`
2. `mill -i node.docker-build && mill -i connector.docker-build`

Note, the steps above do not build an up to date copy of the credentials-verification-web front-end. It is usually
better to run the backend components using docker-compose then run the front-end with npm configuring it to use
the docker backend (how to do that is described in the credentials-verification-web project itself).
 
## Run compose
1. Run ```./compose.sh```

## Adding new components
If one needs to add new components to the CVP project please add it to the docker-compose.yml file.
