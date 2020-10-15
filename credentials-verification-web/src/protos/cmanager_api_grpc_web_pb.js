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
 *   !proto.io.iohk.atala.prism.protos.CreateCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateCredentialResponse>}
 */
const methodDescriptor_CredentialsService_CreateCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/CreateCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateCredentialRequest,
  proto.io.iohk.atala.prism.protos.CreateCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateCredentialRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateCredentialResponse>}
 */
const methodInfo_CredentialsService_CreateCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateCredentialResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateCredentialRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.createCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.createCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetCredentialsResponse>}
 */
const methodDescriptor_CredentialsService_GetCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.CredentialsService/GetCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetCredentialsRequest,
  proto.io.iohk.atala.prism.protos.GetCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetCredentialsResponse>}
 */
const methodInfo_CredentialsService_GetCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.CredentialsServiceClient.prototype.getCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.CredentialsServicePromiseClient.prototype.getCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials);
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
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient =
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
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.CreateStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateStudentResponse>}
 */
const methodDescriptor_StudentsService_CreateStudent = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.StudentsService/CreateStudent',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateStudentRequest,
  proto.io.iohk.atala.prism.protos.CreateStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateStudentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateStudentResponse>}
 */
const methodInfo_StudentsService_CreateStudent = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateStudentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateStudentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateStudentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient.prototype.createStudent =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/CreateStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_CreateStudent,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateStudentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient.prototype.createStudent =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/CreateStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_CreateStudent);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetStudentsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentsResponse>}
 */
const methodDescriptor_StudentsService_GetStudents = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.StudentsService/GetStudents',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetStudentsRequest,
  proto.io.iohk.atala.prism.protos.GetStudentsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetStudentsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentsResponse>}
 */
const methodInfo_StudentsService_GetStudents = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetStudentsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetStudentsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetStudentsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient.prototype.getStudents =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudents',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudents,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetStudentsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient.prototype.getStudents =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudents',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudents);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentResponse>}
 */
const methodDescriptor_StudentsService_GetStudent = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.StudentsService/GetStudent',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetStudentRequest,
  proto.io.iohk.atala.prism.protos.GetStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentResponse>}
 */
const methodInfo_StudentsService_GetStudent = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetStudentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetStudentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient.prototype.getStudent =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudent,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetStudentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient.prototype.getStudent =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudent);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse>}
 */
const methodDescriptor_StudentsService_GetStudentCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.StudentsService/GetStudentCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest,
  proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse>}
 */
const methodInfo_StudentsService_GetStudentCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient.prototype.getStudentCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudentCredentials',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudentCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetStudentCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetStudentCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient.prototype.getStudentCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GetStudentCredentials',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudentCredentials);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse>}
 */
const methodDescriptor_StudentsService_GenerateConnectionTokenForStudent = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.StudentsService/GenerateConnectionTokenForStudent',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse>}
 */
const methodInfo_StudentsService_GenerateConnectionTokenForStudent = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.StudentsServiceClient.prototype.generateConnectionTokenForStudent =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GenerateConnectionTokenForStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GenerateConnectionTokenForStudent,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForStudentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.StudentsServicePromiseClient.prototype.generateConnectionTokenForStudent =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.StudentsService/GenerateConnectionTokenForStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GenerateConnectionTokenForStudent);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient =
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
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient =
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
 *   !proto.io.iohk.atala.prism.protos.CreateSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateSubjectResponse>}
 */
const methodDescriptor_SubjectsService_CreateSubject = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.SubjectsService/CreateSubject',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.CreateSubjectRequest,
  proto.io.iohk.atala.prism.protos.CreateSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateSubjectResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.CreateSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.CreateSubjectResponse>}
 */
const methodInfo_SubjectsService_CreateSubject = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.CreateSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.CreateSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.CreateSubjectResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.CreateSubjectResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.CreateSubjectResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient.prototype.createSubject =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/CreateSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_CreateSubject,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.CreateSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.CreateSubjectResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient.prototype.createSubject =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/CreateSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_CreateSubject);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectsResponse>}
 */
const methodDescriptor_SubjectsService_GetSubjects = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.SubjectsService/GetSubjects',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetSubjectsRequest,
  proto.io.iohk.atala.prism.protos.GetSubjectsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectsResponse>}
 */
const methodInfo_SubjectsService_GetSubjects = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetSubjectsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetSubjectsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetSubjectsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient.prototype.getSubjects =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubjects',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubjects,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetSubjectsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient.prototype.getSubjects =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubjects',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubjects);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectResponse>}
 */
const methodDescriptor_SubjectsService_GetSubject = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.SubjectsService/GetSubject',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetSubjectRequest,
  proto.io.iohk.atala.prism.protos.GetSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectResponse>}
 */
const methodInfo_SubjectsService_GetSubject = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetSubjectResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetSubjectResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient.prototype.getSubject =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubject,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetSubjectResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient.prototype.getSubject =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubject);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse>}
 */
const methodDescriptor_SubjectsService_GetSubjectCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.SubjectsService/GetSubjectCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest,
  proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest,
 *   !proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse>}
 */
const methodInfo_SubjectsService_GetSubjectCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient.prototype.getSubjectCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubjectCredentials',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubjectCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GetSubjectCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient.prototype.getSubjectCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GetSubjectCredentials',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GetSubjectCredentials);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse>}
 */
const methodDescriptor_SubjectsService_GenerateConnectionTokenForSubject = new grpc.web.MethodDescriptor(
  '/io.iohk.atala.prism.protos.SubjectsService/GenerateConnectionTokenForSubject',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest,
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest,
 *   !proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse>}
 */
const methodInfo_SubjectsService_GenerateConnectionTokenForSubject = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse,
  /**
   * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.atala.prism.protos.SubjectsServiceClient.prototype.generateConnectionTokenForSubject =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GenerateConnectionTokenForSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GenerateConnectionTokenForSubject,
      callback);
};


/**
 * @param {!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.atala.prism.protos.GenerateConnectionTokenForSubjectResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.atala.prism.protos.SubjectsServicePromiseClient.prototype.generateConnectionTokenForSubject =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.atala.prism.protos.SubjectsService/GenerateConnectionTokenForSubject',
      request,
      metadata || {},
      methodDescriptor_SubjectsService_GenerateConnectionTokenForSubject);
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

