/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');

const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.prism = {};
proto.io.iohk.prism.protos = require('./admin_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.protos.AdminServiceClient =
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
proto.io.iohk.prism.protos.AdminServicePromiseClient =
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
 *   !proto.io.iohk.prism.protos.PopulateDemoDatasetRequest,
 *   !proto.io.iohk.prism.protos.PopulateDemoDatasetResponse>}
 */
const methodDescriptor_AdminService_PopulateDemoDataset = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.protos.AdminService/PopulateDemoDataset',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.protos.PopulateDemoDatasetRequest,
  proto.io.iohk.prism.protos.PopulateDemoDatasetResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.PopulateDemoDatasetRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.PopulateDemoDatasetResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.protos.PopulateDemoDatasetRequest,
 *   !proto.io.iohk.prism.protos.PopulateDemoDatasetResponse>}
 */
const methodInfo_AdminService_PopulateDemoDataset = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.protos.PopulateDemoDatasetResponse,
  /**
   * @param {!proto.io.iohk.prism.protos.PopulateDemoDatasetRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.protos.PopulateDemoDatasetResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.protos.PopulateDemoDatasetRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.protos.PopulateDemoDatasetResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.protos.PopulateDemoDatasetResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.protos.AdminServiceClient.prototype.populateDemoDataset =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.protos.AdminService/PopulateDemoDataset',
      request,
      metadata || {},
      methodDescriptor_AdminService_PopulateDemoDataset,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.protos.PopulateDemoDatasetRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.protos.PopulateDemoDatasetResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.protos.AdminServicePromiseClient.prototype.populateDemoDataset =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.protos.AdminService/PopulateDemoDataset',
      request,
      metadata || {},
      methodDescriptor_AdminService_PopulateDemoDataset);
};


module.exports = proto.io.iohk.prism.protos;

