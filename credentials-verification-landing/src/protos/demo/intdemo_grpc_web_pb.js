/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.intdemo
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
proto.io.iohk.cvp.intdemo = require('./intdemo_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.intdemo.IDServiceClient =
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
proto.io.iohk.cvp.intdemo.IDServicePromiseClient =
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
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodDescriptor_IDService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodInfo_IDService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_IDService_GetSubjectStatus = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/GetSubjectStatus',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_IDService_GetSubjectStatus = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.getSubjectStatus =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatus',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatus,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.getSubjectStatus =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatus',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatus);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_IDService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_IDService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /** @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


module.exports = proto.io.iohk.cvp.intdemo;

