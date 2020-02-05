/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');

const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.cvp = require('./admin_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.AdminServiceClient =
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
proto.io.iohk.cvp.AdminServicePromiseClient =
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
 *   !proto.io.iohk.cvp.PopulateDemoDatasetRequest,
 *   !proto.io.iohk.cvp.PopulateDemoDatasetResponse>}
 */
const methodDescriptor_AdminService_PopulateDemoDataset = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.AdminService/PopulateDemoDataset',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.PopulateDemoDatasetRequest,
  proto.io.iohk.cvp.PopulateDemoDatasetResponse,
  /**
   * @param {!proto.io.iohk.cvp.PopulateDemoDatasetRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.PopulateDemoDatasetResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.PopulateDemoDatasetRequest,
 *   !proto.io.iohk.cvp.PopulateDemoDatasetResponse>}
 */
const methodInfo_AdminService_PopulateDemoDataset = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.PopulateDemoDatasetResponse,
  /**
   * @param {!proto.io.iohk.cvp.PopulateDemoDatasetRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.PopulateDemoDatasetResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.PopulateDemoDatasetRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.PopulateDemoDatasetResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.PopulateDemoDatasetResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.AdminServiceClient.prototype.populateDemoDataset =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.AdminService/PopulateDemoDataset',
      request,
      metadata || {},
      methodDescriptor_AdminService_PopulateDemoDataset,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.PopulateDemoDatasetRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.PopulateDemoDatasetResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.AdminServicePromiseClient.prototype.populateDemoDataset =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.AdminService/PopulateDemoDataset',
      request,
      metadata || {},
      methodDescriptor_AdminService_PopulateDemoDataset);
};


module.exports = proto.io.iohk.cvp;

