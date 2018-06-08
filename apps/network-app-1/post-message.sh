curl \
  -i \
  -H "Content-Type: application/json" \
  -X POST \
  --data '{"message":"'$1'"}' \
  "http://$2/message"
