#!/bin/bash

curl -d '{"correlationId": "agreementId","data": "it is raining","to": ["2222","3333"]}' -XPOST -H "Content-Type: application/json" http://localhost:8000/agreements/weather/propose
