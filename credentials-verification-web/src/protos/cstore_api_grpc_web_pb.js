/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var cstore_models_pb = require('./cstore_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.prism = {};
proto.io.iohk.prism.protos = require('./cstore_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient =
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
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient =
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
 *   !proto.io.iohk.prism.protos.CreateIndividualRequest,
 *   !proto.io.iohk.prism.protos.CreateIndividualResponse>}
 */
const methodDescriptor_CredentialsStoreService_CreateIndividual = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/CreateIndividual',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.CreateIndividualRequest,
  proto.io.iohk.prism.protos.CreateIndividualResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateIndividualRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateIndividualResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.CreateIndividualRequest,
 *   !proto.io.iohk.prism.protos.CreateIndividualResponse>}
 */
const methodInfo_CredentialsStoreService_CreateIndividual = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.CreateIndividualResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateIndividualRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateIndividualResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.CreateIndividualRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.CreateIndividualResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.CreateIndividualResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.createIndividual =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/CreateIndividual',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateIndividual,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.CreateIndividualRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.CreateIndividualResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.createIndividual =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/CreateIndividual',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateIndividual);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.CreateHolderRequest,
 *   !proto.io.iohk.prism.protos.CreateHolderResponse>}
 */
const methodDescriptor_CredentialsStoreService_CreateHolder = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/CreateHolder',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.CreateHolderRequest,
  proto.io.iohk.prism.protos.CreateHolderResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateHolderRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateHolderResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.CreateHolderRequest,
 *   !proto.io.iohk.prism.protos.CreateHolderResponse>}
 */
const methodInfo_CredentialsStoreService_CreateHolder = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.CreateHolderResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateHolderRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateHolderResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.CreateHolderRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.CreateHolderResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.CreateHolderResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.createHolder =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/CreateHolder',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateHolder,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.CreateHolderRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.CreateHolderResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.createHolder =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/CreateHolder',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_CreateHolder);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetIndividualsRequest,
 *   !proto.io.iohk.prism.protos.GetIndividualsResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetIndividuals = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/GetIndividuals',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetIndividualsRequest,
  proto.io.iohk.prism.protos.GetIndividualsResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetIndividualsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetIndividualsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetIndividualsRequest,
 *   !proto.io.iohk.prism.protos.GetIndividualsResponse>}
 */
const methodInfo_CredentialsStoreService_GetIndividuals = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetIndividualsResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetIndividualsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetIndividualsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetIndividualsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetIndividualsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetIndividualsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.getIndividuals =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GetIndividuals',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetIndividuals,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetIndividualsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetIndividualsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.getIndividuals =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GetIndividuals',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetIndividuals);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest,
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse>}
 */
const methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/GenerateConnectionTokenFor',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest,
  proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest,
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse>}
 */
const methodInfo_CredentialsStoreService_GenerateConnectionTokenFor = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.generateConnectionTokenFor =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GenerateConnectionTokenFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GenerateConnectionTokenForResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.generateConnectionTokenFor =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GenerateConnectionTokenFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GenerateConnectionTokenFor);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.StoreCredentialRequest,
 *   !proto.io.iohk.prism.protos.StoreCredentialResponse>}
 */
const methodDescriptor_CredentialsStoreService_StoreCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/StoreCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.StoreCredentialRequest,
  proto.io.iohk.prism.protos.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.StoreCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.StoreCredentialRequest,
 *   !proto.io.iohk.prism.protos.StoreCredentialResponse>}
 */
const methodInfo_CredentialsStoreService_StoreCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.StoreCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.StoreCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.StoreCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.storeCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.StoreCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.storeCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.prism.protos.GetStoredCredentialsForResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetStoredCredentialsForRequest,
  proto.io.iohk.prism.protos.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.prism.protos.GetStoredCredentialsForResponse>}
 */
const methodInfo_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetStoredCredentialsForResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetStoredCredentialsForResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.CredentialsStoreServiceClient.prototype.getStoredCredentialsFor =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetStoredCredentialsForResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.CredentialsStoreServicePromiseClient.prototype.getStoredCredentialsFor =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor);
};


module.exports = proto.io.iohk.prism.protos;

