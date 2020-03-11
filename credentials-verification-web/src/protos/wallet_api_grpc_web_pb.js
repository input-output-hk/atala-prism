/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var node_models_pb = require('./node_models_pb.js')

var wallet_models_pb = require('./wallet_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.prism = {};
proto.io.iohk.prism.protos = require('./wallet_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.protos.WalletServiceClient =
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
proto.io.iohk.prism.protos.WalletServicePromiseClient =
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
 *   !proto.io.iohk.prism.protos.GetDIDRequest,
 *   !proto.io.iohk.prism.protos.GetDIDResponse>}
 */
const methodDescriptor_WalletService_GetDID = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/GetDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetDIDRequest,
  proto.io.iohk.prism.protos.GetDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetDIDRequest,
 *   !proto.io.iohk.prism.protos.GetDIDResponse>}
 */
const methodInfo_WalletService_GetDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.getDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GetDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetDID,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.getDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GetDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.SignMessageRequest,
 *   !proto.io.iohk.prism.protos.SignMessageResponse>}
 */
const methodDescriptor_WalletService_SignMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/SignMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.SignMessageRequest,
  proto.io.iohk.prism.protos.SignMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.SignMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.SignMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.SignMessageRequest,
 *   !proto.io.iohk.prism.protos.SignMessageResponse>}
 */
const methodInfo_WalletService_SignMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.SignMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.SignMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.SignMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.SignMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.SignMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.SignMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.signMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/SignMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_SignMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.SignMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.SignMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.signMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/SignMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_SignMessage);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.VerifySignedMessageRequest,
 *   !proto.io.iohk.prism.protos.VerifySignedMessageResponse>}
 */
const methodDescriptor_WalletService_VerifySignedMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/VerifySignedMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.VerifySignedMessageRequest,
  proto.io.iohk.prism.protos.VerifySignedMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.VerifySignedMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.VerifySignedMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.VerifySignedMessageRequest,
 *   !proto.io.iohk.prism.protos.VerifySignedMessageResponse>}
 */
const methodInfo_WalletService_VerifySignedMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.VerifySignedMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.VerifySignedMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.VerifySignedMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.VerifySignedMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.VerifySignedMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.VerifySignedMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.verifySignedMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/VerifySignedMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_VerifySignedMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.VerifySignedMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.VerifySignedMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.verifySignedMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/VerifySignedMessage',
      request,
      metadata || {},
      methodDescriptor_WalletService_VerifySignedMessage);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.CreateWalletRequest,
 *   !proto.io.iohk.prism.protos.CreateWalletResponse>}
 */
const methodDescriptor_WalletService_CreateWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/CreateWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.CreateWalletRequest,
  proto.io.iohk.prism.protos.CreateWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.CreateWalletRequest,
 *   !proto.io.iohk.prism.protos.CreateWalletResponse>}
 */
const methodInfo_WalletService_CreateWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.CreateWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.CreateWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.CreateWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.CreateWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.CreateWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.CreateWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.createWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/CreateWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_CreateWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.CreateWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.CreateWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.createWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/CreateWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_CreateWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetWalletStatusRequest,
 *   !proto.io.iohk.prism.protos.GetWalletStatusResponse>}
 */
const methodDescriptor_WalletService_GetWalletStatus = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/GetWalletStatus',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetWalletStatusRequest,
  proto.io.iohk.prism.protos.GetWalletStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetWalletStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetWalletStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetWalletStatusRequest,
 *   !proto.io.iohk.prism.protos.GetWalletStatusResponse>}
 */
const methodInfo_WalletService_GetWalletStatus = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetWalletStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetWalletStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetWalletStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetWalletStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetWalletStatusResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetWalletStatusResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.getWalletStatus =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GetWalletStatus',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetWalletStatus,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetWalletStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetWalletStatusResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.getWalletStatus =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GetWalletStatus',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetWalletStatus);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.UnlockWalletRequest,
 *   !proto.io.iohk.prism.protos.UnlockWalletResponse>}
 */
const methodDescriptor_WalletService_unlockWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/unlockWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.UnlockWalletRequest,
  proto.io.iohk.prism.protos.UnlockWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.UnlockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.UnlockWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.UnlockWalletRequest,
 *   !proto.io.iohk.prism.protos.UnlockWalletResponse>}
 */
const methodInfo_WalletService_unlockWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.UnlockWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.UnlockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.UnlockWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.UnlockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.UnlockWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.UnlockWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.unlockWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/unlockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_unlockWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.UnlockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.UnlockWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.unlockWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/unlockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_unlockWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.LockWalletRequest,
 *   !proto.io.iohk.prism.protos.LockWalletResponse>}
 */
const methodDescriptor_WalletService_lockWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/lockWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.LockWalletRequest,
  proto.io.iohk.prism.protos.LockWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.LockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.LockWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.LockWalletRequest,
 *   !proto.io.iohk.prism.protos.LockWalletResponse>}
 */
const methodInfo_WalletService_lockWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.LockWalletResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.LockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.LockWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.LockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.LockWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.LockWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.lockWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/lockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_lockWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.LockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.LockWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.lockWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/lockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_lockWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.ChangePassphraseRequest,
 *   !proto.io.iohk.prism.protos.ChangePassphraseResponse>}
 */
const methodDescriptor_WalletService_changePassphrase = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/changePassphrase',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.ChangePassphraseRequest,
  proto.io.iohk.prism.protos.ChangePassphraseResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ChangePassphraseRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ChangePassphraseResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.ChangePassphraseRequest,
 *   !proto.io.iohk.prism.protos.ChangePassphraseResponse>}
 */
const methodInfo_WalletService_changePassphrase = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.ChangePassphraseResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ChangePassphraseRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ChangePassphraseResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.ChangePassphraseRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.ChangePassphraseResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.ChangePassphraseResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.changePassphrase =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/changePassphrase',
      request,
      metadata || {},
      methodDescriptor_WalletService_changePassphrase,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.ChangePassphraseRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.ChangePassphraseResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.changePassphrase =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/changePassphrase',
      request,
      metadata || {},
      methodDescriptor_WalletService_changePassphrase);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GenerateDIDRequest,
 *   !proto.io.iohk.prism.protos.GenerateDIDResponse>}
 */
const methodDescriptor_WalletService_GenerateDID = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.WalletService/GenerateDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GenerateDIDRequest,
  proto.io.iohk.prism.protos.GenerateDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GenerateDIDRequest,
 *   !proto.io.iohk.prism.protos.GenerateDIDResponse>}
 */
const methodInfo_WalletService_GenerateDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GenerateDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GenerateDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GenerateDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GenerateDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.WalletServiceClient.prototype.generateDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GenerateDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GenerateDID,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GenerateDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GenerateDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.WalletServicePromiseClient.prototype.generateDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.WalletService/GenerateDID',
      request,
      metadata || {},
      methodDescriptor_WalletService_GenerateDID);
};


module.exports = proto.io.iohk.prism.protos;

