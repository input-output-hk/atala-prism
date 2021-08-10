/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.prism.intdemo.protos
 * @enhanceable
 * @public
 */

// GENERATED CODE -- DO NOT EDIT!



const grpc = {};
grpc.web = require('grpc-web');


var intdemo_models_pb = require('./intdemo_models_pb.js')
const proto = {};
proto.io = {};
proto.io.iohk = {};
proto.io.iohk.prism = {};
proto.io.iohk.prism.intdemo = {};
proto.io.iohk.prism.intdemo.protos = require('./intdemo_api_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.intdemo.protos.IDServiceClient =
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
proto.io.iohk.prism.intdemo.protos.IDServicePromiseClient =
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
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodDescriptor_IDService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.IDService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodInfo_IDService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.IDServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.intdemo.protos.IDServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodDescriptor_IDService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.IDService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodInfo_IDService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.IDServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.IDServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest,
 *   !proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse>}
 */
const methodDescriptor_IDService_SetPersonalData = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.IDService/SetPersonalData',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest,
  proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest,
 *   !proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse>}
 */
const methodInfo_IDService_SetPersonalData = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.IDServiceClient.prototype.setPersonalData =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/SetPersonalData',
      request,
      metadata || {},
      methodDescriptor_IDService_SetPersonalData,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.SetPersonalDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.intdemo.protos.SetPersonalDataResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.intdemo.protos.IDServicePromiseClient.prototype.setPersonalData =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.IDService/SetPersonalData',
      request,
      metadata || {},
      methodDescriptor_IDService_SetPersonalData);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.intdemo.protos.DegreeServiceClient =
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
proto.io.iohk.prism.intdemo.protos.DegreeServicePromiseClient =
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
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodDescriptor_DegreeService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.DegreeService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodInfo_DegreeService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.DegreeServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.DegreeService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.intdemo.protos.DegreeServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.DegreeService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodDescriptor_DegreeService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.DegreeService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodInfo_DegreeService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.DegreeServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.DegreeService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.DegreeServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.DegreeService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetSubjectStatusStream);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.intdemo.protos.EmploymentServiceClient =
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
proto.io.iohk.prism.intdemo.protos.EmploymentServicePromiseClient =
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
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodDescriptor_EmploymentService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.EmploymentService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodInfo_EmploymentService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.EmploymentServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.EmploymentService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.intdemo.protos.EmploymentServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.EmploymentService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodDescriptor_EmploymentService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.EmploymentService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodInfo_EmploymentService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.EmploymentServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.EmploymentService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.EmploymentServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.EmploymentService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetSubjectStatusStream);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.prism.intdemo.protos.InsuranceServiceClient =
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
proto.io.iohk.prism.intdemo.protos.InsuranceServicePromiseClient =
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
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodDescriptor_InsuranceService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.InsuranceService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 */
const methodInfo_InsuranceService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.InsuranceServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.InsuranceService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.prism.intdemo.protos.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.prism.intdemo.protos.InsuranceServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.InsuranceService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodDescriptor_InsuranceService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.prism.intdemo.protos.InsuranceService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest,
 *   !proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 */
const methodInfo_InsuranceService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.InsuranceServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.InsuranceService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.prism.intdemo.protos.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.prism.intdemo.protos.InsuranceServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.prism.intdemo.protos.InsuranceService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetSubjectStatusStream);
};


module.exports = proto.io.iohk.prism.intdemo.protos;

