#!/bin/bash

curl -d '{"correlationId": "agreementId","data": "it is raining"}' -XPOST -H "Content-Type: application/json" http://localhost:9000/agreements/weather/agree
