curl --user ${CIRCLECI_TOKEN} \
  --request POST \
  --form revision=bc849eb67256fa3646d75e5e23e2a4addb55a9cf \
  --form config=@config.yml \
  --form notify=false \
    https://circleci.com/api/v1.1/project/github/input-output-hk/cardano-enterprise/experiment%2Fbase-network
