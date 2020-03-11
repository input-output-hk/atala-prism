/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var connector_models_pb = require('./connector_models_pb.js')

var node_models_pb = require('./node_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.prism = {};
proto.io.iohk.prism.protos = require('./connector_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.protos.ConnectorServiceClient =
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
proto.io.iohk.prism.protos.ConnectorServicePromiseClient =
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
 *   !proto.io.iohk.prism.protos.GetConnectionByTokenRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionByTokenResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionByToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetConnectionByToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetConnectionByTokenRequest,
  proto.io.iohk.prism.protos.GetConnectionByTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionByTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionByTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetConnectionByTokenRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionByTokenResponse>}
 */
const methodInfo_ConnectorService_GetConnectionByToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetConnectionByTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionByTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionByTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionByTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetConnectionByTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetConnectionByTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getConnectionByToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionByToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionByToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionByTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetConnectionByTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getConnectionByToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionByToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionByToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionsPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetConnectionsPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest,
  proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetConnectionsPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getConnectionsPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionsPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetConnectionsPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getConnectionsPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionsPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionsPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse>}
 */
const methodDescriptor_ConnectorService_GetConnectionTokenInfo = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetConnectionTokenInfo',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest,
  proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest,
 *   !proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse>}
 */
const methodInfo_ConnectorService_GetConnectionTokenInfo = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getConnectionTokenInfo =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetConnectionTokenInfoRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetConnectionTokenInfoResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getConnectionTokenInfo =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetConnectionTokenInfo',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetConnectionTokenInfo);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.prism.protos.AddConnectionFromTokenResponse>}
 */
const methodDescriptor_ConnectorService_AddConnectionFromToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/AddConnectionFromToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.AddConnectionFromTokenRequest,
  proto.io.iohk.prism.protos.AddConnectionFromTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.AddConnectionFromTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.AddConnectionFromTokenRequest,
 *   !proto.io.iohk.prism.protos.AddConnectionFromTokenResponse>}
 */
const methodInfo_ConnectorService_AddConnectionFromToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.AddConnectionFromTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.AddConnectionFromTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.AddConnectionFromTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.AddConnectionFromTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.AddConnectionFromTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.addConnectionFromToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.AddConnectionFromTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.AddConnectionFromTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.addConnectionFromToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/AddConnectionFromToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_AddConnectionFromToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.DeleteConnectionRequest,
 *   !proto.io.iohk.prism.protos.DeleteConnectionResponse>}
 */
const methodDescriptor_ConnectorService_DeleteConnection = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/DeleteConnection',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.DeleteConnectionRequest,
  proto.io.iohk.prism.protos.DeleteConnectionResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.DeleteConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.DeleteConnectionResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.DeleteConnectionRequest,
 *   !proto.io.iohk.prism.protos.DeleteConnectionResponse>}
 */
const methodInfo_ConnectorService_DeleteConnection = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.DeleteConnectionResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.DeleteConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.DeleteConnectionResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.DeleteConnectionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.DeleteConnectionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.deleteConnection =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.DeleteConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.DeleteConnectionResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.deleteConnection =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/DeleteConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_DeleteConnection);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.RegisterDIDRequest,
 *   !proto.io.iohk.prism.protos.RegisterDIDResponse>}
 */
const methodDescriptor_ConnectorService_RegisterDID = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/RegisterDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.RegisterDIDRequest,
  proto.io.iohk.prism.protos.RegisterDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.RegisterDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.RegisterDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.RegisterDIDRequest,
 *   !proto.io.iohk.prism.protos.RegisterDIDResponse>}
 */
const methodInfo_ConnectorService_RegisterDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.RegisterDIDResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.RegisterDIDRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.RegisterDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.RegisterDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.RegisterDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.registerDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.RegisterDIDRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.RegisterDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.registerDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/RegisterDID',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_RegisterDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.ChangeBillingPlanRequest,
 *   !proto.io.iohk.prism.protos.ChangeBillingPlanResponse>}
 */
const methodDescriptor_ConnectorService_ChangeBillingPlan = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/ChangeBillingPlan',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.ChangeBillingPlanRequest,
  proto.io.iohk.prism.protos.ChangeBillingPlanResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ChangeBillingPlanRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.ChangeBillingPlanRequest,
 *   !proto.io.iohk.prism.protos.ChangeBillingPlanResponse>}
 */
const methodInfo_ConnectorService_ChangeBillingPlan = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.ChangeBillingPlanResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ChangeBillingPlanRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ChangeBillingPlanResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.ChangeBillingPlanResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.ChangeBillingPlanResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.changeBillingPlan =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.ChangeBillingPlanRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.ChangeBillingPlanResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.changeBillingPlan =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/ChangeBillingPlan',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ChangeBillingPlan);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenResponse>}
 */
const methodDescriptor_ConnectorService_GenerateConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GenerateConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GenerateConnectionTokenRequest,
  proto.io.iohk.prism.protos.GenerateConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.prism.protos.GenerateConnectionTokenResponse>}
 */
const methodInfo_ConnectorService_GenerateConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GenerateConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GenerateConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GenerateConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.generateConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GenerateConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.generateConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GenerateConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.prism.protos.GetMessagesPaginatedResponse>}
 */
const methodDescriptor_ConnectorService_GetMessagesPaginated = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetMessagesPaginated',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetMessagesPaginatedRequest,
  proto.io.iohk.prism.protos.GetMessagesPaginatedResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetMessagesPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetMessagesPaginatedRequest,
 *   !proto.io.iohk.prism.protos.GetMessagesPaginatedResponse>}
 */
const methodInfo_ConnectorService_GetMessagesPaginated = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetMessagesPaginatedResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetMessagesPaginatedRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetMessagesPaginatedResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetMessagesPaginatedResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetMessagesPaginatedResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getMessagesPaginated =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetMessagesPaginatedRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetMessagesPaginatedResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getMessagesPaginated =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetMessagesPaginated',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesPaginated);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetMessagesForConnectionRequest,
 *   !proto.io.iohk.prism.protos.GetMessagesForConnectionResponse>}
 */
const methodDescriptor_ConnectorService_GetMessagesForConnection = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetMessagesForConnection',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetMessagesForConnectionRequest,
  proto.io.iohk.prism.protos.GetMessagesForConnectionResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetMessagesForConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetMessagesForConnectionResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetMessagesForConnectionRequest,
 *   !proto.io.iohk.prism.protos.GetMessagesForConnectionResponse>}
 */
const methodInfo_ConnectorService_GetMessagesForConnection = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetMessagesForConnectionResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetMessagesForConnectionRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetMessagesForConnectionResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetMessagesForConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetMessagesForConnectionResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetMessagesForConnectionResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getMessagesForConnection =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetMessagesForConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesForConnection,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetMessagesForConnectionRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetMessagesForConnectionResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getMessagesForConnection =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetMessagesForConnection',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetMessagesForConnection);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.SendMessageRequest,
 *   !proto.io.iohk.prism.protos.SendMessageResponse>}
 */
const methodDescriptor_ConnectorService_SendMessage = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/SendMessage',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.SendMessageRequest,
  proto.io.iohk.prism.protos.SendMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.SendMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.SendMessageResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.SendMessageRequest,
 *   !proto.io.iohk.prism.protos.SendMessageResponse>}
 */
const methodInfo_ConnectorService_SendMessage = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.SendMessageResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.SendMessageRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.SendMessageResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.SendMessageResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.SendMessageResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.sendMessage =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.SendMessageRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.SendMessageResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.sendMessage =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/SendMessage',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_SendMessage);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest,
 *   !proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse>}
 */
const methodDescriptor_ConnectorService_GetBraintreePaymentsConfig = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetBraintreePaymentsConfig',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest,
  proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest,
 *   !proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse>}
 */
const methodInfo_ConnectorService_GetBraintreePaymentsConfig = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getBraintreePaymentsConfig =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetBraintreePaymentsConfig',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetBraintreePaymentsConfig,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetBraintreePaymentsConfigResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getBraintreePaymentsConfig =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetBraintreePaymentsConfig',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetBraintreePaymentsConfig);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.ProcessPaymentRequest,
 *   !proto.io.iohk.prism.protos.ProcessPaymentResponse>}
 */
const methodDescriptor_ConnectorService_ProcessPayment = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/ProcessPayment',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.ProcessPaymentRequest,
  proto.io.iohk.prism.protos.ProcessPaymentResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ProcessPaymentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ProcessPaymentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.ProcessPaymentRequest,
 *   !proto.io.iohk.prism.protos.ProcessPaymentResponse>}
 */
const methodInfo_ConnectorService_ProcessPayment = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.ProcessPaymentResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.ProcessPaymentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.ProcessPaymentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.ProcessPaymentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.ProcessPaymentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.ProcessPaymentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.processPayment =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/ProcessPayment',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ProcessPayment,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.ProcessPaymentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.ProcessPaymentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.processPayment =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/ProcessPayment',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_ProcessPayment);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.protos.GetPaymentsRequest,
 *   !proto.io.iohk.prism.protos.GetPaymentsResponse>}
 */
const methodDescriptor_ConnectorService_GetPayments = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.ConnectorService/GetPayments',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.GetPaymentsRequest,
  proto.io.iohk.prism.protos.GetPaymentsResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetPaymentsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetPaymentsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.GetPaymentsRequest,
 *   !proto.io.iohk.prism.protos.GetPaymentsResponse>}
 */
const methodInfo_ConnectorService_GetPayments = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.GetPaymentsResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.GetPaymentsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.GetPaymentsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.GetPaymentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.GetPaymentsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.GetPaymentsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.ConnectorServiceClient.prototype.getPayments =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetPayments',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetPayments,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.GetPaymentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.GetPaymentsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.ConnectorServicePromiseClient.prototype.getPayments =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.ConnectorService/GetPayments',
      request,
      metadata || {},
      methodDescriptor_ConnectorService_GetPayments);
};


module.exports = proto.io.iohk.prism.protos;

