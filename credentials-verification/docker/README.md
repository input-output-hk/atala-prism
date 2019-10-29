# Building and running a docker image for CVP

## Main Docker file for the whole CVP project
To build and run CVP docker image per component one needs to follow these steps:
1. Go to main CVP directory (atala/credentials-verification)
2. Execute ```docker build -t cvp -f docker/Dockerfile .```
3. Execute ```docker run -it cvp:latest bash -c out/COMPONENT_NAME/launcher/dest/run``` where a COMPONENT_NAME can be ``` connector```

## Generating docker-compose.yml with external dependencies
If one needs to create the CVP project with external dependencies one may use docker-compose.yml file which contains the whole CVP project (for now just connector) and all external dependencies (for now just postgres)
To build and run docker-compose one needs to follow these steps:
1. Go to main CVP directory (atala/credentials-verification)
2. Execute ```docker-compose -f docker/docker-compose.yml up```
*NOTE:* if one needs to do some modifications in the CVP docker image and apply them to docker-compose first make these changes in the build and then execute ```docker-compose -f docker/docker-compose.yml down``` and then ```docker-compose -f docker/docker-compose.yml up --build --force-recreate```

## Adding new components
If one needs to add new components to the CVP project feel free to add it to the docker-compose.yml file.
