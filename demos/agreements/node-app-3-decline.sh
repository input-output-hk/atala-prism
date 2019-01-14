#!/bin/bash

curl -d '{"correlationId": "agreementId"}' -XPOST -H "Content-Type: application/json" http://localhost:7000/agreements/weather/decline