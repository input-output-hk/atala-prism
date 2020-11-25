# Docs website
This has the necessary stuff to prepare the docs website for deployment.

The docker image simplifies deploying the website from circleci.

## Run locally
Follow the [instructions](../../README.md#Website) to build the website, then:
- Move it to this directory so that docker can find it: `mv ../../docs/target/site/ website`
- Build the docker image: `docker build -t prism-docs-website .`
- Run the container: `docker run -p 8000:80 -it prism-docs-website`
- Go to [localhost:8000](http://localhost:8000) to find the website.

## Misc
For now, the docs are protected by http basic authentication, `demo:iohk4ever`, if you ever need to update this password:
- Run `htpasswd -n demo` to define the new password (install `apache2-utils` if the command wasn't found).
- Paste the output line to [htpasswd](nginx/htpasswd).
