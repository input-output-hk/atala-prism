/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.connector
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
proto.io.iohk.cvp.connector = require('./connector_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient =
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
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient =
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
 *   !proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionsPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GetConnectionsPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest,
  proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetConnectionsPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.getConnectionsPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GetConnectionsPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.getConnectionsPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionTokenInfo = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GetConnectionTokenInfo',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest,
  proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse>}
 */
const methodInfo_ConnectorService_GetConnectionTokenInfo = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.getConnectionTokenInfo =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GetConnectionTokenInfoResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.getConnectionTokenInfo =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse>}
 */
const methodDescriptor_ConnectorService_AddConnectionFromToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/AddConnectionFromToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest,
  proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse>}
 */
const methodInfo_ConnectorService_AddConnectionFromToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.addConnectionFromToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.AddConnectionFromTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.addConnectionFromToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.DeleteConnectionRequest,
 *   !proto.io.iohk.cvp.connector.DeleteConnectionResponse>}
 */
const methodDescriptor_ConnectorService_DeleteConnection = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/DeleteConnection',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.DeleteConnectionRequest,
  proto.io.iohk.cvp.connector.DeleteConnectionResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.DeleteConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.DeleteConnectionResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.DeleteConnectionRequest,
 *   !proto.io.iohk.cvp.connector.DeleteConnectionResponse>}
 */
const methodInfo_ConnectorService_DeleteConnection = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.DeleteConnectionResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.DeleteConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.DeleteConnectionResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.DeleteConnectionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.DeleteConnectionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.deleteConnection =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.DeleteConnectionResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.deleteConnection =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.RegisterDIDRequest,
 *   !proto.io.iohk.cvp.connector.RegisterDIDResponse>}
 */
const methodDescriptor_ConnectorService_RegisterDID = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/RegisterDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.RegisterDIDRequest,
  proto.io.iohk.cvp.connector.RegisterDIDResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.RegisterDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.RegisterDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.RegisterDIDRequest,
 *   !proto.io.iohk.cvp.connector.RegisterDIDResponse>}
 */
const methodInfo_ConnectorService_RegisterDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.RegisterDIDResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.RegisterDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.RegisterDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.RegisterDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.RegisterDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.registerDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.RegisterDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.registerDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.ChangeBillingPlanRequest,
 *   !proto.io.iohk.cvp.connector.ChangeBillingPlanResponse>}
 */
const methodDescriptor_ConnectorService_ChangeBillingPlan = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/ChangeBillingPlan',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.ChangeBillingPlanRequest,
  proto.io.iohk.cvp.connector.ChangeBillingPlanResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.ChangeBillingPlanRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.ChangeBillingPlanRequest,
 *   !proto.io.iohk.cvp.connector.ChangeBillingPlanResponse>}
 */
const methodInfo_ConnectorService_ChangeBillingPlan = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.ChangeBillingPlanResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.ChangeBillingPlanRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.ChangeBillingPlanResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.ChangeBillingPlanResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.changeBillingPlan =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.ChangeBillingPlanResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.changeBillingPlan =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse>}
 */
const methodDescriptor_ConnectorService_GenerateConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GenerateConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest,
  proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse>}
 */
const methodInfo_ConnectorService_GenerateConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.generateConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GenerateConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.generateConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetMessagesPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GetMessagesPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest,
  proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetMessagesPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.getMessagesPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GetMessagesPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.getMessagesPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest,
 *   !proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse>}
 */
const methodDescriptor_ConnectorService_GetMessagesForConnection = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GetMessagesForConnection',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest,
  proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest,
 *   !proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse>}
 */
const methodInfo_ConnectorService_GetMessagesForConnection = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.getMessagesForConnection =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetMessagesForConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesForConnection,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GetMessagesForConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GetMessagesForConnectionResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.getMessagesForConnection =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GetMessagesForConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesForConnection);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.SendMessageRequest,
 *   !proto.io.iohk.cvp.connector.SendMessageResponse>}
 */
const methodDescriptor_ConnectorService_SendMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/SendMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.SendMessageRequest,
  proto.io.iohk.cvp.connector.SendMessageResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.SendMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.SendMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.SendMessageRequest,
 *   !proto.io.iohk.cvp.connector.SendMessageResponse>}
 */
const methodInfo_ConnectorService_SendMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.SendMessageResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.SendMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.SendMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.SendMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.SendMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.sendMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.SendMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.sendMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest,
 *   !proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse>}
 */
const methodDescriptor_ConnectorService_GeneratePaymentUrl = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.connector.ConnectorService/GeneratePaymentUrl',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest,
  proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest,
 *   !proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse>}
 */
const methodInfo_ConnectorService_GeneratePaymentUrl = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse,
  /**
   * @param {!proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.connector.ConnectorServiceClient.prototype.generatePaymentUrl =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GeneratePaymentUrl',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GeneratePaymentUrl,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.connector.GeneratePaymentUrlRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.connector.GeneratePaymentUrlResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.connector.ConnectorServicePromiseClient.prototype.generatePaymentUrl =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.connector.ConnectorService/GeneratePaymentUrl',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GeneratePaymentUrl);
};


module.exports = proto.io.iohk.cvp.connector;

