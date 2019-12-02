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
  /**
   * @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request
   * @return {!Uint8Array}
   */
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
  /**
   * @param {!proto.io.iohk.cvp.wallet.GetDIDRequest} request
   * @return {!Uint8Array}
   */
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
  /**
   * @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request
   * @return {!Uint8Array}
   */
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
  /**
   * @param {!proto.io.iohk.cvp.wallet.SignMessageRequest} request
   * @return {!Uint8Array}
   */
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
  /**
   * @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request
   * @return {!Uint8Array}
   */
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
  /**
   * @param {!proto.io.iohk.cvp.wallet.VerifySignedMessageRequest} request
   * @return {!Uint8Array}
   */
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


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.CreateWalletRequest,
 *   !proto.io.iohk.cvp.wallet.CreateWalletResponse>}
 */
const methodDescriptor_WalletService_CreateWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/CreateWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.CreateWalletRequest,
  proto.io.iohk.cvp.wallet.CreateWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.CreateWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.CreateWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.CreateWalletRequest,
 *   !proto.io.iohk.cvp.wallet.CreateWalletResponse>}
 */
const methodInfo_WalletService_CreateWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.CreateWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.CreateWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.CreateWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.CreateWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.CreateWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.CreateWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.createWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/CreateWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_CreateWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.CreateWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.CreateWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.createWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/CreateWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_CreateWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.GetWalletStatusRequest,
 *   !proto.io.iohk.cvp.wallet.GetWalletStatusResponse>}
 */
const methodDescriptor_WalletService_GetWalletStatus = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/GetWalletStatus',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.GetWalletStatusRequest,
  proto.io.iohk.cvp.wallet.GetWalletStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.GetWalletStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.GetWalletStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.GetWalletStatusRequest,
 *   !proto.io.iohk.cvp.wallet.GetWalletStatusResponse>}
 */
const methodInfo_WalletService_GetWalletStatus = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.GetWalletStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.GetWalletStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.GetWalletStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.GetWalletStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.GetWalletStatusResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.GetWalletStatusResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.getWalletStatus =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/GetWalletStatus',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetWalletStatus,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.GetWalletStatusRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.GetWalletStatusResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.getWalletStatus =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/GetWalletStatus',
      request,
      metadata || {},
      methodDescriptor_WalletService_GetWalletStatus);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.UnlockWalletRequest,
 *   !proto.io.iohk.cvp.wallet.UnlockWalletResponse>}
 */
const methodDescriptor_WalletService_unlockWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/unlockWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.UnlockWalletRequest,
  proto.io.iohk.cvp.wallet.UnlockWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.UnlockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.UnlockWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.UnlockWalletRequest,
 *   !proto.io.iohk.cvp.wallet.UnlockWalletResponse>}
 */
const methodInfo_WalletService_unlockWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.UnlockWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.UnlockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.UnlockWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.UnlockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.UnlockWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.UnlockWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.unlockWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/unlockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_unlockWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.UnlockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.UnlockWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.unlockWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/unlockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_unlockWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.LockWalletRequest,
 *   !proto.io.iohk.cvp.wallet.LockWalletResponse>}
 */
const methodDescriptor_WalletService_lockWallet = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/lockWallet',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.LockWalletRequest,
  proto.io.iohk.cvp.wallet.LockWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.LockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.LockWalletResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.LockWalletRequest,
 *   !proto.io.iohk.cvp.wallet.LockWalletResponse>}
 */
const methodInfo_WalletService_lockWallet = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.LockWalletResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.LockWalletRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.LockWalletResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.LockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.LockWalletResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.LockWalletResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.lockWallet =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/lockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_lockWallet,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.LockWalletRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.LockWalletResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.lockWallet =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/lockWallet',
      request,
      metadata || {},
      methodDescriptor_WalletService_lockWallet);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.wallet.ChangePassphraseRequest,
 *   !proto.io.iohk.cvp.wallet.ChangePassphraseResponse>}
 */
const methodDescriptor_WalletService_changePassphrase = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.wallet.WalletService/changePassphrase',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.wallet.ChangePassphraseRequest,
  proto.io.iohk.cvp.wallet.ChangePassphraseResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.ChangePassphraseRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.ChangePassphraseResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.wallet.ChangePassphraseRequest,
 *   !proto.io.iohk.cvp.wallet.ChangePassphraseResponse>}
 */
const methodInfo_WalletService_changePassphrase = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.wallet.ChangePassphraseResponse,
  /**
   * @param {!proto.io.iohk.cvp.wallet.ChangePassphraseRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.wallet.ChangePassphraseResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.wallet.ChangePassphraseRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.wallet.ChangePassphraseResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.wallet.ChangePassphraseResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.wallet.WalletServiceClient.prototype.changePassphrase =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/changePassphrase',
      request,
      metadata || {},
      methodDescriptor_WalletService_changePassphrase,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.wallet.ChangePassphraseRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.wallet.ChangePassphraseResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.wallet.WalletServicePromiseClient.prototype.changePassphrase =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.wallet.WalletService/changePassphrase',
      request,
      metadata || {},
      methodDescriptor_WalletService_changePassphrase);
};


module.exports = proto.io.iohk.cvp.wallet;

