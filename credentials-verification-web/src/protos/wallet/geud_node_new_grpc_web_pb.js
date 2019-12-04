/* eslint-disable */
/**
 * @fileoverview gRPC-Web ---generated client stub for io.iohk.nodenew
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');

const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.nodenew = require('./geud_node_new_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.nodenew.NodeServiceClient =
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
proto.io.iohk.nodenew.NodeServicePromiseClient =
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
 *   !proto.io.iohk.nodenew.GetDidDocumentRequest,
 *   !proto.io.iohk.nodenew.GetDidDocumentResponse>}
 */
const methodDescriptor_NodeService_GetDidDocument = new grpc.web.MethodDescriptor(
  '/io.iohk.nodenew.NodeService/GetDidDocument',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.nodenew.GetDidDocumentRequest,
  proto.io.iohk.nodenew.GetDidDocumentResponse,
  /**
   * @param {!proto.io.iohk.nodenew.GetDidDocumentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.GetDidDocumentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.nodenew.GetDidDocumentRequest,
 *   !proto.io.iohk.nodenew.GetDidDocumentResponse>}
 */
const methodInfo_NodeService_GetDidDocument = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.nodenew.GetDidDocumentResponse,
  /**
   * @param {!proto.io.iohk.nodenew.GetDidDocumentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.GetDidDocumentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.nodenew.GetDidDocumentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.nodenew.GetDidDocumentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.nodenew.GetDidDocumentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.nodenew.NodeServiceClient.prototype.getDidDocument =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/GetDidDocument',
      request,
      metadata || {},
      methodDescriptor_NodeService_GetDidDocument,
      callback);
};


/**
 * @param {!proto.io.iohk.nodenew.GetDidDocumentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.nodenew.GetDidDocumentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.nodenew.NodeServicePromiseClient.prototype.getDidDocument =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/GetDidDocument',
      request,
      metadata || {},
      methodDescriptor_NodeService_GetDidDocument);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.CreateDIDResponse>}
 */
const methodDescriptor_NodeService_CreateDID = new grpc.web.MethodDescriptor(
  '/io.iohk.nodenew.NodeService/CreateDID',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.nodenew.SignedAtalaOperation,
  proto.io.iohk.nodenew.CreateDIDResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.CreateDIDResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.CreateDIDResponse>}
 */
const methodInfo_NodeService_CreateDID = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.nodenew.CreateDIDResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.CreateDIDResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.nodenew.CreateDIDResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.nodenew.CreateDIDResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.nodenew.NodeServiceClient.prototype.createDID =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/CreateDID',
      request,
      metadata || {},
      methodDescriptor_NodeService_CreateDID,
      callback);
};


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.nodenew.CreateDIDResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.nodenew.NodeServicePromiseClient.prototype.createDID =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/CreateDID',
      request,
      metadata || {},
      methodDescriptor_NodeService_CreateDID);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.IssueCredentialResponse>}
 */
const methodDescriptor_NodeService_IssueCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.nodenew.NodeService/IssueCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.nodenew.SignedAtalaOperation,
  proto.io.iohk.nodenew.IssueCredentialResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.IssueCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.IssueCredentialResponse>}
 */
const methodInfo_NodeService_IssueCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.nodenew.IssueCredentialResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.IssueCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.nodenew.IssueCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.nodenew.IssueCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.nodenew.NodeServiceClient.prototype.issueCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/IssueCredential',
      request,
      metadata || {},
      methodDescriptor_NodeService_IssueCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.nodenew.IssueCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.nodenew.NodeServicePromiseClient.prototype.issueCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/IssueCredential',
      request,
      metadata || {},
      methodDescriptor_NodeService_IssueCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.RevokeCredentialResponse>}
 */
const methodDescriptor_NodeService_RevokeCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.nodenew.NodeService/RevokeCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.nodenew.SignedAtalaOperation,
  proto.io.iohk.nodenew.RevokeCredentialResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.RevokeCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.nodenew.SignedAtalaOperation,
 *   !proto.io.iohk.nodenew.RevokeCredentialResponse>}
 */
const methodInfo_NodeService_RevokeCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.nodenew.RevokeCredentialResponse,
  /**
   * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.nodenew.RevokeCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.nodenew.RevokeCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.nodenew.RevokeCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.nodenew.NodeServiceClient.prototype.revokeCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/RevokeCredential',
      request,
      metadata || {},
      methodDescriptor_NodeService_RevokeCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.nodenew.SignedAtalaOperation} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.nodenew.RevokeCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.nodenew.NodeServicePromiseClient.prototype.revokeCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.nodenew.NodeService/RevokeCredential',
      request,
      metadata || {},
      methodDescriptor_NodeService_RevokeCredential);
};


module.exports = proto.io.iohk.nodenew;

