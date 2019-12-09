#!/bin/bash

protoc --python_out=. -I ../cmanager/protobuf/ ../cmanager/protobuf/credential.proto
python3 fake_data.py
