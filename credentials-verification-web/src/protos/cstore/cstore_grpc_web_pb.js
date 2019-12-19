/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.cstore
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');

const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.cvp = {};
proto.io.iohk.cvp.cstore = require('./cstore_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options['format'] = 'text';

  /**
   * @private @const {!grpc.web.GrpcWebClientBase} The client
   */
  this.client_ = new grpc.web.GrpcWebClientBase(options);

  /**
   * @private @const {string} The hostname
   */
  this.hostname_ = hostname;

};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient =
    function(hostname, credentials, options) {
  if (!options) options = {};
  options['format'] = 'text';

  /**
   * @private @const {!grpc.web.GrpcWebClientBase} The client
   */
  this.client_ = new grpc.web.GrpcWebClientBase(options);

  /**
   * @private @const {string} The hostname
   */
  this.hostname_ = hostname;

};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.RegisterRequest,
 *   !proto.io.iohk.cvp.cstore.RegisterResponse>}
 */
const methodDescriptor_CredentialsStoreService_Register = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/Register',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.RegisterRequest,
  proto.io.iohk.cvp.cstore.RegisterResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.RegisterRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.RegisterResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.RegisterRequest,
 *   !proto.io.iohk.cvp.cstore.RegisterResponse>}
 */
const methodInfo_CredentialsStoreService_Register = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.RegisterResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.RegisterRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.RegisterResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.RegisterRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.RegisterResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.RegisterResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.register =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/Register',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_Register,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.RegisterRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.RegisterResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.register =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/Register',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_Register);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.CreateIndividualRequest,
 *   !proto.io.iohk.cvp.cstore.CreateIndividualResponse>}
 */
const methodDescriptor_CredentialsStoreService_CreateIndividual = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/CreateIndividual',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.CreateIndividualRequest,
  proto.io.iohk.cvp.cstore.CreateIndividualResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.CreateIndividualRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.CreateIndividualResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.CreateIndividualRequest,
 *   !proto.io.iohk.cvp.cstore.CreateIndividualResponse>}
 */
const methodInfo_CredentialsStoreService_CreateIndividual = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.CreateIndividualResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.CreateIndividualRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.CreateIndividualResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.CreateIndividualRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.CreateIndividualResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.CreateIndividualResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.createIndividual =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/CreateIndividual',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateIndividual,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.CreateIndividualRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.CreateIndividualResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.createIndividual =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/CreateIndividual',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateIndividual);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.GetIndividualsRequest,
 *   !proto.io.iohk.cvp.cstore.GetIndividualsResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetIndividuals = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/GetIndividuals',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.GetIndividualsRequest,
  proto.io.iohk.cvp.cstore.GetIndividualsResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GetIndividualsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GetIndividualsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.GetIndividualsRequest,
 *   !proto.io.iohk.cvp.cstore.GetIndividualsResponse>}
 */
const methodInfo_CredentialsStoreService_GetIndividuals = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.GetIndividualsResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GetIndividualsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GetIndividualsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.GetIndividualsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.GetIndividualsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.GetIndividualsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.getIndividuals =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GetIndividuals',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetIndividuals,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.GetIndividualsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.GetIndividualsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.getIndividuals =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GetIndividuals',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetIndividuals);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest,
 *   !proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse>}
 */
const methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/GenerateConnectionTokenFor',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest,
  proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest,
 *   !proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse>}
 */
const methodInfo_CredentialsStoreService_GenerateConnectionTokenFor = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.generateConnectionTokenFor =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GenerateConnectionTokenFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.GenerateConnectionTokenForResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.generateConnectionTokenFor =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GenerateConnectionTokenFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.StoreCredentialRequest,
 *   !proto.io.iohk.cvp.cstore.StoreCredentialResponse>}
 */
const methodDescriptor_CredentialsStoreService_StoreCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/StoreCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.StoreCredentialRequest,
  proto.io.iohk.cvp.cstore.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.StoreCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.StoreCredentialRequest,
 *   !proto.io.iohk.cvp.cstore.StoreCredentialResponse>}
 */
const methodInfo_CredentialsStoreService_StoreCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.StoreCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.StoreCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.StoreCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.storeCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.StoreCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.storeCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cstore.CredentialsStoreService/GetStoredCredentialsFor',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest,
  proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse>}
 */
const methodInfo_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServiceClient.prototype.getStoredCredentialsFor =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cstore.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cstore.GetStoredCredentialsForResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cstore.CredentialsStoreServicePromiseClient.prototype.getStoredCredentialsFor =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cstore.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor);
};


module.exports = proto.io.iohk.cvp.cstore;

