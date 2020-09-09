/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.atala.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var cviews_models_pb = require('./cviews_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.atala = {};
proto.io.iohk.atala.prism = {};
proto.io.iohk.atala.prism.protos = require('./cviews_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.CredentialViewsServiceClient =
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
proto.io.iohk.atala.prism.protos.CredentialViewsServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest,
 *   !proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse>}
 */
const methodDescriptor_CredentialViewsService_GetCredentialViewTemplates = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialViewsService/GetCredentialViewTemplates',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest,
  proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest,
 *   !proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse>}
 */
const methodInfo_CredentialViewsService_GetCredentialViewTemplates = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialViewsServiceClient.prototype.getCredentialViewTemplates =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialViewsService/GetCredentialViewTemplates',
      request,
      metadata || {},
      methodDescriptor_CredentialViewsService_GetCredentialViewTemplates,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetCredentialViewTemplatesResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialViewsServicePromiseClient.prototype.getCredentialViewTemplates =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialViewsService/GetCredentialViewTemplates',
      request,
      metadata || {},
      methodDescriptor_CredentialViewsService_GetCredentialViewTemplates);
};


module.exports = proto.io.iohk.atala.prism.protos;

