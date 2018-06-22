curl \
  -i \
  -H "Content-Type: application/json" \
  -X POST \
  --data '{"message":"'$1'", "expectedPeerCount":'$2'}' \
  "http://$3/message"
