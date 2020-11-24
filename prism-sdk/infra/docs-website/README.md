# Docs website
This has the necessary stuff to prepare the docs website for deployment.

The docker image simplifies deploying the website from circleci.

## Run locally
Follow the [instructions](../../README.md#Website) to build the website, then:
- Move it to this directory so that docker can find it: `mv ../../docs/target/site/ website`
- Build the docker image: `docker build -t prism-docs-website .`
- Run the container: `docker run -p 8000:80 -it prism-docs-website`
- Go to [localhost:8000](http://localhost:8000) to find the website.
