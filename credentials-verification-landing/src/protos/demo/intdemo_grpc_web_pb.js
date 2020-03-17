/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.intdemo
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
proto.io.iohk.cvp.intdemo = require('./intdemo_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.intdemo.IDServiceClient =
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
proto.io.iohk.cvp.intdemo.IDServicePromiseClient =
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
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodDescriptor_IDService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodInfo_IDService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_IDService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_IDService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_IDService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_IDService_GetSubjectStatusStream);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.SetPersonalDataRequest,
 *   !proto.io.iohk.cvp.intdemo.SetPersonalDataResponse>}
 */
const methodDescriptor_IDService_SetPersonalData = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.IDService/SetPersonalData',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.SetPersonalDataRequest,
  proto.io.iohk.cvp.intdemo.SetPersonalDataResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.SetPersonalDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.SetPersonalDataResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.SetPersonalDataRequest,
 *   !proto.io.iohk.cvp.intdemo.SetPersonalDataResponse>}
 */
const methodInfo_IDService_SetPersonalData = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.SetPersonalDataResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.SetPersonalDataRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.SetPersonalDataResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.SetPersonalDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.SetPersonalDataResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.SetPersonalDataResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.IDServiceClient.prototype.setPersonalData =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/SetPersonalData',
      request,
      metadata || {},
      methodDescriptor_IDService_SetPersonalData,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.SetPersonalDataRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.SetPersonalDataResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.IDServicePromiseClient.prototype.setPersonalData =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.IDService/SetPersonalData',
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
proto.io.iohk.cvp.intdemo.DegreeServiceClient =
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
proto.io.iohk.cvp.intdemo.DegreeServicePromiseClient =
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
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodDescriptor_DegreeService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.DegreeService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodInfo_DegreeService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.DegreeServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.DegreeService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.DegreeServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.DegreeService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_DegreeService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.DegreeService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_DegreeService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.DegreeServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.DegreeService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_DegreeService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.DegreeServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.DegreeService/GetSubjectStatusStream',
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
proto.io.iohk.cvp.intdemo.EmploymentServiceClient =
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
proto.io.iohk.cvp.intdemo.EmploymentServicePromiseClient =
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
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodDescriptor_EmploymentService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.EmploymentService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodInfo_EmploymentService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.EmploymentServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.EmploymentService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.EmploymentServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.EmploymentService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_EmploymentService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.EmploymentService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_EmploymentService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.EmploymentServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.EmploymentService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_EmploymentService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.EmploymentServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.EmploymentService/GetSubjectStatusStream',
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
proto.io.iohk.cvp.intdemo.InsuranceServiceClient =
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
proto.io.iohk.cvp.intdemo.InsuranceServicePromiseClient =
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
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodDescriptor_InsuranceService_GetConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.InsuranceService/GetConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest,
 *   !proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 */
const methodInfo_InsuranceService_GetConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.InsuranceServiceClient.prototype.getConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.InsuranceService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.intdemo.GetConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.intdemo.InsuranceServicePromiseClient.prototype.getConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.intdemo.InsuranceService/GetConnectionToken',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetConnectionToken);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodDescriptor_InsuranceService_GetSubjectStatusStream = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.intdemo.InsuranceService/GetSubjectStatusStream',
  grpc.web.MethodType.SERVER_STREAMING,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest,
 *   !proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 */
const methodInfo_InsuranceService_GetSubjectStatusStream = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse,
  /**
   * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request
   * @return {!Uint8Array}
   */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.InsuranceServiceClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.InsuranceService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetSubjectStatusStream);
};


/**
 * @param {!proto.io.iohk.cvp.intdemo.GetSubjectStatusRequest} request The request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.intdemo.GetSubjectStatusResponse>}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.intdemo.InsuranceServicePromiseClient.prototype.getSubjectStatusStream =
    function(request, metadata) {
  return this.client_.serverStreaming(this.hostname_ +
      '/io.iohk.cvp.intdemo.InsuranceService/GetSubjectStatusStream',
      request,
      metadata || {},
      methodDescriptor_InsuranceService_GetSubjectStatusStream);
};


module.exports = proto.io.iohk.cvp.intdemo;

