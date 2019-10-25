# Building and running a docker image for CVP
To build and run CVP docker image per component one needs to follow these steps:
1. Go to main CVP directory (atala/credentials-verification)
2. Execute ```docker build -t cvp -f docker/Dockerfile .```
3. Execute ```docker run -it cvp:latest bash -c out/COMPONENT_NAME/launcher/dest/run``` where a COMPONENT_NAME can be ``` connector```
