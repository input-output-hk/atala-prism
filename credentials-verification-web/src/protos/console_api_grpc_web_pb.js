/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.atala.prism.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var console_models_pb = require('./console_models_pb.js')

var cmanager_models_pb = require('./cmanager_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.atala = {};
proto.io.iohk.atala.prism = {};
proto.io.iohk.atala.prism.protos = require('./console_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.ConsoleServiceClient =
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
proto.io.iohk.atala.prism.protos.ConsoleServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.CreateContactRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateContactResponse>}
 */
const methodDescriptor_ConsoleService_CreateContact = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.ConsoleService/CreateContact',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateContactRequest,
  proto.io.iohk.atala.prism.protos.CreateContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateContactResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateContactRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateContactResponse>}
 */
const methodInfo_ConsoleService_CreateContact = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateContactResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateContactResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateContactResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.ConsoleServiceClient.prototype.createContact =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/CreateContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_CreateContact,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateContactResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.ConsoleServicePromiseClient.prototype.createContact =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/CreateContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_CreateContact);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetContactsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactsResponse>}
 */
const methodDescriptor_ConsoleService_GetContacts = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.ConsoleService/GetContacts',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetContactsRequest,
  proto.io.iohk.atala.prism.protos.GetContactsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetContactsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactsResponse>}
 */
const methodInfo_ConsoleService_GetContacts = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetContactsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetContactsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetContactsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.ConsoleServiceClient.prototype.getContacts =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GetContacts',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GetContacts,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetContactsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.ConsoleServicePromiseClient.prototype.getContacts =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GetContacts',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GetContacts);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetContactRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactResponse>}
 */
const methodDescriptor_ConsoleService_GetContact = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.ConsoleService/GetContact',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetContactRequest,
  proto.io.iohk.atala.prism.protos.GetContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetContactRequest,
 *   !proto.io.iohk.atala.prism.protos.GetContactResponse>}
 */
const methodInfo_ConsoleService_GetContact = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetContactResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetContactResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetContactResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.ConsoleServiceClient.prototype.getContact =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GetContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GetContact,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetContactResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.ConsoleServicePromiseClient.prototype.getContact =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GetContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GetContact);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse>}
 */
const methodDescriptor_ConsoleService_GenerateConnectionTokenForContact = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.ConsoleService/GenerateConnectionTokenForContact',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse>}
 */
const methodInfo_ConsoleService_GenerateConnectionTokenForContact = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.ConsoleServiceClient.prototype.generateConnectionTokenForContact =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GenerateConnectionTokenForContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GenerateConnectionTokenForContact,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForContactResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.ConsoleServicePromiseClient.prototype.generateConnectionTokenForContact =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.ConsoleService/GenerateConnectionTokenForContact',
      request,
      metadata || {},
      methodDescriptor_ConsoleService_GenerateConnectionTokenForContact);
};


module.exports = proto.io.iohk.atala.prism.protos;

