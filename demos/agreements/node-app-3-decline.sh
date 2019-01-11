#!/bin/bash

curl -d '{"correlationId": "agreementId"}' -XPOST -H "Content-Type: application/json" http://localhost:9000/agreements/weather/decline
