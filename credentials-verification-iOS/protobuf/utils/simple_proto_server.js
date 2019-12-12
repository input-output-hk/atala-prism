/*
 *
 * Copyright 2015 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

const PROTO_PATH = __dirname + "/connector_user.proto";

const grpc = require("grpc");
const protoLoader = require("@grpc/proto-loader");
const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});
const connector_proto = grpc.loadPackageDefinition(packageDefinition).connector;

var counter = 0;

const getConnections = (call, callback) => {
  console.log("getConnections reached");
  return callback(null, {
    connections: [{
      connectionId: "someId",
      issuerInfo: {
        DID: "someDID",
        name: "someName"
      }
    }]
  });
}

const getConnectionTokenInfo = (call, callback) => {
  console.log("getConnections reached");
  console.log("request: ", call.request);
  counter ++;
  return callback(null,{
    issuer:{
      DID: `${counter}`,
      name: `Received Token: ${call.request.token}`
    }
  });
}

const addConnectionFromToken = (call, callback) => {
  const { token } = call;
  console.log("AddConnectionFromToken, token: ", token);
    return callback(null, {
    connection: {
      connectionId: "someId",
      issuerInfo: {
        DID: "someDID",
        name: "someName"
      }
    }
  });
}

const deleteConnection = (call, callback) => {
  const { connectionId } = call;
  console.log("DeleteConnection, connectionId: ", connectionId);
  return callback(null, {});
}

const getCredentialsSince = (call, callback) => {
  const { since, limit } = call;
  console.log("GetCredentialsSince, since: ", since);
  console.log("GetCredentialsSince, limit: ", limit);
  return callback(null, {
    received: "received!",
    issuerInfo: {
      DID: "someDID",
      name: "someName"
    },
    subject: "someSubject",
    title: "someTitle",
    type: 2,
    credentialData: 0
  });
}


/**
 * Starts an RPC server that receives requests for the Greeter service at the
 * sample server port
 */
const main = () => {
  var server = new grpc.Server();
  server.addService(connector_proto.ConnectorUserService.service, {
    getConnections,
    getConnectionTokenInfo,
    addConnectionFromToken,
    deleteConnection,
    getCredentialsSince
  });
  server.bind("0.0.0.0:50051", grpc.ServerCredentials.createInsecure());
  server.start();
}

main();
