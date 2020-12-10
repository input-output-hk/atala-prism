/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.atala.prism.protos
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
proto.io.iohk.atala = {};
proto.io.iohk.atala.prism = {};
proto.io.iohk.atala.prism.protos = require('./cstore_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServiceClient =
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
proto.io.iohk.atala.prism.protos.CredentialsStoreServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.StoreCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.StoreCredentialResponse>}
 */
const methodDescriptor_CredentialsStoreService_StoreCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsStoreService/StoreCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.StoreCredentialRequest,
  proto.io.iohk.atala.prism.protos.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.StoreCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.StoreCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.StoreCredentialResponse>}
 */
const methodInfo_CredentialsStoreService_StoreCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.StoreCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.StoreCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.StoreCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.StoreCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.StoreCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServiceClient.prototype.storeCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.StoreCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.StoreCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServicePromiseClient.prototype.storeCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/StoreCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_StoreCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest,
  proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse>}
 */
const methodInfo_CredentialsStoreService_GetStoredCredentialsFor = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServiceClient.prototype.getStoredCredentialsFor =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetStoredCredentialsForResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServicePromiseClient.prototype.getStoredCredentialsFor =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/GetStoredCredentialsFor',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetStoredCredentialsFor);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest,
 *   !proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse>}
 */
const methodDescriptor_CredentialsStoreService_GetLatestCredentialExternalId = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsStoreService/GetLatestCredentialExternalId',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest,
  proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest,
 *   !proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse>}
 */
const methodInfo_CredentialsStoreService_GetLatestCredentialExternalId = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServiceClient.prototype.getLatestCredentialExternalId =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/GetLatestCredentialExternalId',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetLatestCredentialExternalId,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetLatestCredentialExternalIdResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsStoreServicePromiseClient.prototype.getLatestCredentialExternalId =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsStoreService/GetLatestCredentialExternalId',
      request,
      metadata || {},
      methodDescriptor_CredentialsStoreService_GetLatestCredentialExternalId);
};


module.exports = proto.io.iohk.atala.prism.protos;

