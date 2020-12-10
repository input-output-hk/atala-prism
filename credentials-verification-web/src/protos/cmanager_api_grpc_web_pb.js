/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.atala.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var cmanager_models_pb = require('./cmanager_models_pb.js')

var common_models_pb = require('./common_models_pb.js')

var node_models_pb = require('./node_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.atala = {};
proto.io.iohk.atala.prism = {};
proto.io.iohk.atala.prism.protos = require('./cmanager_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient =
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
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse>}
 */
const methodDescriptor_CredentialsService_CreateGenericCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/CreateGenericCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest,
  proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse>}
 */
const methodInfo_CredentialsService_CreateGenericCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.createGenericCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/CreateGenericCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateGenericCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateGenericCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateGenericCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.createGenericCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/CreateGenericCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateGenericCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse>}
 */
const methodDescriptor_CredentialsService_GetGenericCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/GetGenericCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest,
  proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse>}
 */
const methodInfo_CredentialsService_GetGenericCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.getGenericCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetGenericCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetGenericCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetGenericCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetGenericCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.getGenericCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetGenericCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetGenericCredentials);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse>}
 */
const methodDescriptor_CredentialsService_GetContactCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/GetContactCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest,
  proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse>}
 */
const methodInfo_CredentialsService_GetContactCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.getContactCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetContactCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetContactCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetContactCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.getContactCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetContactCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetContactCredentials);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.PublishCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.PublishCredentialResponse>}
 */
const methodDescriptor_CredentialsService_PublishCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/PublishCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.PublishCredentialRequest,
  proto.io.iohk.atala.prism.protos.PublishCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.PublishCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.PublishCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.PublishCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.PublishCredentialResponse>}
 */
const methodInfo_CredentialsService_PublishCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.PublishCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.PublishCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.PublishCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.PublishCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.PublishCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.PublishCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.publishCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/PublishCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_PublishCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.PublishCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.PublishCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.publishCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/PublishCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_PublishCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.ShareCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.ShareCredentialResponse>}
 */
const methodDescriptor_CredentialsService_ShareCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/ShareCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.ShareCredentialRequest,
  proto.io.iohk.atala.prism.protos.ShareCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.ShareCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.ShareCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.ShareCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.ShareCredentialResponse>}
 */
const methodInfo_CredentialsService_ShareCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.ShareCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.ShareCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.ShareCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.ShareCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.ShareCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.ShareCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.shareCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/ShareCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_ShareCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.ShareCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.ShareCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.shareCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/ShareCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_ShareCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest,
 *   !proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse>}
 */
const methodDescriptor_CredentialsService_GetBlockchainData = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/GetBlockchainData',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest,
  proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest,
 *   !proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse>}
 */
const methodInfo_CredentialsService_GetBlockchainData = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.getBlockchainData =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetBlockchainData',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetBlockchainData,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetBlockchainDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetBlockchainDataResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.getBlockchainData =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetBlockchainData',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetBlockchainData);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.GroupsServiceClient =
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
proto.io.iohk.atala.prism.protos.GroupsServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.CreateGroupRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateGroupResponse>}
 */
const methodDescriptor_GroupsService_CreateGroup = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.GroupsService/CreateGroup',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateGroupRequest,
  proto.io.iohk.atala.prism.protos.CreateGroupResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateGroupRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateGroupResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateGroupRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateGroupResponse>}
 */
const methodInfo_GroupsService_CreateGroup = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateGroupResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateGroupRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateGroupResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateGroupResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateGroupResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.GroupsServiceClient.prototype.createGroup =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/CreateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_CreateGroup,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateGroupResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.GroupsServicePromiseClient.prototype.createGroup =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/CreateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_CreateGroup);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetGroupsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetGroupsResponse>}
 */
const methodDescriptor_GroupsService_GetGroups = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.GroupsService/GetGroups',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetGroupsRequest,
  proto.io.iohk.atala.prism.protos.GetGroupsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetGroupsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetGroupsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetGroupsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetGroupsResponse>}
 */
const methodInfo_GroupsService_GetGroups = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetGroupsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetGroupsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetGroupsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetGroupsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetGroupsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetGroupsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.GroupsServiceClient.prototype.getGroups =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/GetGroups',
      request,
      metadata || {},
      methodDescriptor_GroupsService_GetGroups,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetGroupsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetGroupsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.GroupsServicePromiseClient.prototype.getGroups =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/GetGroups',
      request,
      metadata || {},
      methodDescriptor_GroupsService_GetGroups);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.UpdateGroupRequest,
 *   !proto.io.iohk.atala.prism.protos.UpdateGroupResponse>}
 */
const methodDescriptor_GroupsService_UpdateGroup = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.GroupsService/UpdateGroup',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.UpdateGroupRequest,
  proto.io.iohk.atala.prism.protos.UpdateGroupResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.UpdateGroupRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.UpdateGroupResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.UpdateGroupRequest,
 *   !proto.io.iohk.atala.prism.protos.UpdateGroupResponse>}
 */
const methodInfo_GroupsService_UpdateGroup = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.UpdateGroupResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.UpdateGroupRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.UpdateGroupResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.UpdateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.UpdateGroupResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.UpdateGroupResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.GroupsServiceClient.prototype.updateGroup =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/UpdateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_UpdateGroup,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.UpdateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.UpdateGroupResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.GroupsServicePromiseClient.prototype.updateGroup =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.GroupsService/UpdateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_UpdateGroup);
};


module.exports = proto.io.iohk.atala.prism.protos;

