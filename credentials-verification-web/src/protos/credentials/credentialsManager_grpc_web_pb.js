/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.cmanager
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
proto.io.iohk.cvp.cmanager = require('./credentialsManager_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 */
const methodDescriptor_CredentialsService_CreateCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 */
const methodInfo_CredentialsService_CreateCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.CreateCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.CreateCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient.prototype.createCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient.prototype.createCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 */
const methodDescriptor_CredentialsService_GetCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 */
const methodInfo_CredentialsService_GetCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient.prototype.getCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient.prototype.getCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials);
};


module.exports = proto.io.iohk.cvp.cmanager;

