#!/bin/bash

grpcurl -import-path ../protobuf -proto ../protobuf/admin.proto -plaintext localhost:50055 io.iohk.cvp.AdminService/PopulateDemoDataSet