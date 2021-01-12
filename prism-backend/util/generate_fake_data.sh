#!/bin/bash

protoc --python_out=. -I ../connector/protobuf/credential/ ../connector/protobuf/credential/credential.proto
python3 fake_data.py
