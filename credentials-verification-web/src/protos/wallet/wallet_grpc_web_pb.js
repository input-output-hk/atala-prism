/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.wallet
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
proto.io.iohk.cvp.wallet = require('./wallet_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.wallet.WalletServiceClient =
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
proto.io.iohk.cvp.wallet.WalletServicePromiseClient =
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
 *   !proto.io.iohk.cvp.wallet.GetDIDRequest,
 *   !proto.io.iohk.cvp.wallet.GetDIDResponse>}
 */
const methodDescriptor_WalletService_GetDID = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/GetDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.GetDIDRequest,
  proto.io.iohk.cvp.wallet.GetDIDResponse,
  /** @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.GetDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.GetDIDRequest,
 *   !proto.io.iohk.cvp.wallet.GetDIDResponse>}
 */
const methodInfo_WalletService_GetDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.GetDIDResponse,
  /** @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.GetDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.GetDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.GetDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.getDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/GetDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetDID,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.GetDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.getDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/GetDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.SignMessageRequest,
 *   !proto.io.iohk.cvp.wallet.SignMessageResponse>}
 */
const methodDescriptor_WalletService_SignMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/SignMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.SignMessageRequest,
  proto.io.iohk.cvp.wallet.SignMessageResponse,
  /** @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.SignMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.SignMessageRequest,
 *   !proto.io.iohk.cvp.wallet.SignMessageResponse>}
 */
const methodInfo_WalletService_SignMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.SignMessageResponse,
  /** @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.SignMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.SignMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.SignMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.signMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/SignMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_SignMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.SignMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.signMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/SignMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_SignMessage);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.VerifySignedMessageRequest,
 *   !proto.io.iohk.cvp.wallet.VerifySignedMessageResponse>}
 */
const methodDescriptor_WalletService_VerifySignedMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/VerifySignedMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.VerifySignedMessageRequest,
  proto.io.iohk.cvp.wallet.VerifySignedMessageResponse,
  /** @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.VerifySignedMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.VerifySignedMessageRequest,
 *   !proto.io.iohk.cvp.wallet.VerifySignedMessageResponse>}
 */
const methodInfo_WalletService_VerifySignedMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.VerifySignedMessageResponse,
  /** @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.VerifySignedMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.VerifySignedMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.VerifySignedMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.verifySignedMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/VerifySignedMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_VerifySignedMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.VerifySignedMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.verifySignedMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/VerifySignedMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_VerifySignedMessage);
};


module.exports = proto.io.iohk.cvp.wallet;

