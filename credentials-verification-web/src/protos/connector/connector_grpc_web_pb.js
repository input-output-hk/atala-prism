/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.connector
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var scalapb_pb = require('./scalapb_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.connector = require('./connector_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.connector.ConnectorServiceClient =
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
proto.io.iohk.connector.ConnectorServicePromiseClient =
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
 *   !proto.io.iohk.connector.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.connector.GetConnectionsPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionsPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/GetConnectionsPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.GetConnectionsPaginatedRequest,
  proto.io.iohk.connector.GetConnectionsPaginatedResponse,
  /** @param {!proto.io.iohk.connector.GetConnectionsPaginatedRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.connector.GetConnectionsPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetConnectionsPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.GetConnectionsPaginatedResponse,
  /** @param {!proto.io.iohk.connector.GetConnectionsPaginatedRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.GetConnectionsPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.GetConnectionsPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.getConnectionsPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.GetConnectionsPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.getConnectionsPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.connector.GetConnectionTokenInfoResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionTokenInfo = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/GetConnectionTokenInfo',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.GetConnectionTokenInfoRequest,
  proto.io.iohk.connector.GetConnectionTokenInfoResponse,
  /** @param {!proto.io.iohk.connector.GetConnectionTokenInfoRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.connector.GetConnectionTokenInfoResponse>}
 */
const methodInfo_ConnectorService_GetConnectionTokenInfo = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.GetConnectionTokenInfoResponse,
  /** @param {!proto.io.iohk.connector.GetConnectionTokenInfoRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.GetConnectionTokenInfoResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.GetConnectionTokenInfoResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.getConnectionTokenInfo =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.GetConnectionTokenInfoResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.getConnectionTokenInfo =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.connector.AddConnectionFromTokenResponse>}
 */
const methodDescriptor_ConnectorService_AddConnectionFromToken = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/AddConnectionFromToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.AddConnectionFromTokenRequest,
  proto.io.iohk.connector.AddConnectionFromTokenResponse,
  /** @param {!proto.io.iohk.connector.AddConnectionFromTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.connector.AddConnectionFromTokenResponse>}
 */
const methodInfo_ConnectorService_AddConnectionFromToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.AddConnectionFromTokenResponse,
  /** @param {!proto.io.iohk.connector.AddConnectionFromTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.AddConnectionFromTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.AddConnectionFromTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.addConnectionFromToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.AddConnectionFromTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.addConnectionFromToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.DeleteConnectionRequest,
 *   !proto.io.iohk.connector.DeleteConnectionResponse>}
 */
const methodDescriptor_ConnectorService_DeleteConnection = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/DeleteConnection',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.DeleteConnectionRequest,
  proto.io.iohk.connector.DeleteConnectionResponse,
  /** @param {!proto.io.iohk.connector.DeleteConnectionRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.DeleteConnectionResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.DeleteConnectionRequest,
 *   !proto.io.iohk.connector.DeleteConnectionResponse>}
 */
const methodInfo_ConnectorService_DeleteConnection = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.DeleteConnectionResponse,
  /** @param {!proto.io.iohk.connector.DeleteConnectionRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.DeleteConnectionResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.DeleteConnectionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.DeleteConnectionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.deleteConnection =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.DeleteConnectionResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.deleteConnection =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.RegisterDIDRequest,
 *   !proto.io.iohk.connector.RegisterDIDResponse>}
 */
const methodDescriptor_ConnectorService_RegisterDID = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/RegisterDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.RegisterDIDRequest,
  proto.io.iohk.connector.RegisterDIDResponse,
  /** @param {!proto.io.iohk.connector.RegisterDIDRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.RegisterDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.RegisterDIDRequest,
 *   !proto.io.iohk.connector.RegisterDIDResponse>}
 */
const methodInfo_ConnectorService_RegisterDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.RegisterDIDResponse,
  /** @param {!proto.io.iohk.connector.RegisterDIDRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.RegisterDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.RegisterDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.RegisterDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.registerDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.RegisterDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.registerDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.ChangeBillingPlanRequest,
 *   !proto.io.iohk.connector.ChangeBillingPlanResponse>}
 */
const methodDescriptor_ConnectorService_ChangeBillingPlan = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/ChangeBillingPlan',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.ChangeBillingPlanRequest,
  proto.io.iohk.connector.ChangeBillingPlanResponse,
  /** @param {!proto.io.iohk.connector.ChangeBillingPlanRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.ChangeBillingPlanRequest,
 *   !proto.io.iohk.connector.ChangeBillingPlanResponse>}
 */
const methodInfo_ConnectorService_ChangeBillingPlan = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.ChangeBillingPlanResponse,
  /** @param {!proto.io.iohk.connector.ChangeBillingPlanRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.ChangeBillingPlanResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.ChangeBillingPlanResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.changeBillingPlan =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.ChangeBillingPlanResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.changeBillingPlan =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.connector.GenerateConnectionTokenResponse>}
 */
const methodDescriptor_ConnectorService_GenerateConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/GenerateConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.GenerateConnectionTokenRequest,
  proto.io.iohk.connector.GenerateConnectionTokenResponse,
  /** @param {!proto.io.iohk.connector.GenerateConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.connector.GenerateConnectionTokenResponse>}
 */
const methodInfo_ConnectorService_GenerateConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.GenerateConnectionTokenResponse,
  /** @param {!proto.io.iohk.connector.GenerateConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.GenerateConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.GenerateConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.generateConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.GenerateConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.generateConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.connector.GetMessagesPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetMessagesPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/GetMessagesPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.GetMessagesPaginatedRequest,
  proto.io.iohk.connector.GetMessagesPaginatedResponse,
  /** @param {!proto.io.iohk.connector.GetMessagesPaginatedRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.connector.GetMessagesPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetMessagesPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.GetMessagesPaginatedResponse,
  /** @param {!proto.io.iohk.connector.GetMessagesPaginatedRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.GetMessagesPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.GetMessagesPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.getMessagesPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.GetMessagesPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.getMessagesPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.connector.SendMessageRequest,
 *   !proto.io.iohk.connector.SendMessageResponse>}
 */
const methodDescriptor_ConnectorService_SendMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.connector.ConnectorService/SendMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.connector.SendMessageRequest,
  proto.io.iohk.connector.SendMessageResponse,
  /** @param {!proto.io.iohk.connector.SendMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.SendMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.connector.SendMessageRequest,
 *   !proto.io.iohk.connector.SendMessageResponse>}
 */
const methodInfo_ConnectorService_SendMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.connector.SendMessageResponse,
  /** @param {!proto.io.iohk.connector.SendMessageRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.connector.SendMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.connector.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.connector.SendMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.connector.SendMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.connector.ConnectorServiceClient.prototype.sendMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.connector.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.connector.SendMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.connector.ConnectorServicePromiseClient.prototype.sendMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.connector.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage);
};


module.exports = proto.io.iohk.connector;

