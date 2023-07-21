# Envoy docker image
- Build the image with: `./build.sh -b`
- Push the image with: `./build.sh -p`

## Troubleshooting
- If you get `denied: Your authorization token has expired. Reauthenticate and try again`, run `$(aws ecr get-login --no-include-email)` on your console, which should fix the problem.

