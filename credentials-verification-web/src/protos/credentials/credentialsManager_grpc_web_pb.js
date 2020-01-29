/* eslint-disable */
/**
 * @fileoverview gRPC-Web generated client stub for io.iohk.cvp.cmanager
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
proto.io.iohk.cvp.cmanager = require('./credentialsManager_pb.js');

/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.RegisterRequest,
 *   !proto.io.iohk.cvp.cmanager.RegisterResponse>}
 */
const methodDescriptor_CredentialsService_Register = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.CredentialsService/Register',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.RegisterRequest,
  proto.io.iohk.cvp.cmanager.RegisterResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.RegisterRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.RegisterResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.RegisterRequest,
 *   !proto.io.iohk.cvp.cmanager.RegisterResponse>}
 */
const methodInfo_CredentialsService_Register = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.RegisterResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.RegisterRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.RegisterResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.RegisterRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.RegisterResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.RegisterResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient.prototype.register =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/Register',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_Register,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.RegisterRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.RegisterResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient.prototype.register =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/Register',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_Register);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 */
const methodDescriptor_CredentialsService_CreateCredential = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 */
const methodInfo_CredentialsService_CreateCredential = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateCredentialResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.CreateCredentialResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.CreateCredentialResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient.prototype.createCredential =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateCredentialRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.CreateCredentialResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient.prototype.createCredential =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/CreateCredential',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_CreateCredential);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 */
const methodDescriptor_CredentialsService_GetCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 */
const methodInfo_CredentialsService_GetCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.CredentialsServiceClient.prototype.getCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.CredentialsServicePromiseClient.prototype.getCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.CredentialsService/GetCredentials',
      request,
      metadata || {},
      methodDescriptor_CredentialsService_GetCredentials);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.CreateStudentRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateStudentResponse>}
 */
const methodDescriptor_StudentsService_CreateStudent = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.StudentsService/CreateStudent',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.CreateStudentRequest,
  proto.io.iohk.cvp.cmanager.CreateStudentResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateStudentRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateStudentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.CreateStudentRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateStudentResponse>}
 */
const methodInfo_StudentsService_CreateStudent = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.CreateStudentResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateStudentRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateStudentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.CreateStudentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.CreateStudentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient.prototype.createStudent =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/CreateStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_CreateStudent,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.CreateStudentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient.prototype.createStudent =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/CreateStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_CreateStudent);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetStudentsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentsResponse>}
 */
const methodDescriptor_StudentsService_GetStudents = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.StudentsService/GetStudents',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetStudentsRequest,
  proto.io.iohk.cvp.cmanager.GetStudentsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetStudentsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentsResponse>}
 */
const methodInfo_StudentsService_GetStudents = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetStudentsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetStudentsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetStudentsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient.prototype.getStudents =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudents',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudents,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetStudentsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient.prototype.getStudents =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudents',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudents);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetStudentRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentResponse>}
 */
const methodDescriptor_StudentsService_GetStudent = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.StudentsService/GetStudent',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetStudentRequest,
  proto.io.iohk.cvp.cmanager.GetStudentResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetStudentRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentResponse>}
 */
const methodInfo_StudentsService_GetStudent = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetStudentResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetStudentResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetStudentResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient.prototype.getStudent =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudent,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetStudentResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient.prototype.getStudent =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudent',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudent);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse>}
 */
const methodDescriptor_StudentsService_GetStudentCredentials = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.StudentsService/GetStudentCredentials',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest,
  proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse>}
 */
const methodInfo_StudentsService_GetStudentCredentials = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient.prototype.getStudentCredentials =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudentCredentials',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudentCredentials,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetStudentCredentialsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetStudentCredentialsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient.prototype.getStudentCredentials =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GetStudentCredentials',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GetStudentCredentials);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse>}
 */
const methodDescriptor_StudentsService_GenerateConnectionToken = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.StudentsService/GenerateConnectionToken',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest,
  proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest,
 *   !proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse>}
 */
const methodInfo_StudentsService_GenerateConnectionToken = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.StudentsServiceClient.prototype.generateConnectionToken =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GenerateConnectionToken,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GenerateConnectionTokenResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.StudentsServicePromiseClient.prototype.generateConnectionToken =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.StudentsService/GenerateConnectionToken',
      request,
      metadata || {},
      methodDescriptor_StudentsService_GenerateConnectionToken);
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.GroupsServiceClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @param {string} hostname
 * @param {?Object} credentials
 * @param {?Object} options
 * @constructor
 * @struct
 * @final
 */
proto.io.iohk.cvp.cmanager.GroupsServicePromiseClient =
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

  /**
   * @private @const {?Object} The credentials to be used to connect
   *    to the server
   */
  this.credentials_ = credentials;

  /**
   * @private @const {?Object} Options for the client
   */
  this.options_ = options;
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.CreateGroupRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateGroupResponse>}
 */
const methodDescriptor_GroupsService_CreateGroup = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.GroupsService/CreateGroup',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.CreateGroupRequest,
  proto.io.iohk.cvp.cmanager.CreateGroupResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateGroupRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateGroupResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.CreateGroupRequest,
 *   !proto.io.iohk.cvp.cmanager.CreateGroupResponse>}
 */
const methodInfo_GroupsService_CreateGroup = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.CreateGroupResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.CreateGroupRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.CreateGroupResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.CreateGroupResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.CreateGroupResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.GroupsServiceClient.prototype.createGroup =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.GroupsService/CreateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_CreateGroup,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.CreateGroupRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.CreateGroupResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.GroupsServicePromiseClient.prototype.createGroup =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.GroupsService/CreateGroup',
      request,
      metadata || {},
      methodDescriptor_GroupsService_CreateGroup);
};


/**
 * @const
 * @type {!grpc.web.MethodDescriptor<
 *   !proto.io.iohk.cvp.cmanager.GetGroupsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetGroupsResponse>}
 */
const methodDescriptor_GroupsService_GetGroups = new grpc.web.MethodDescriptor(
  '/io.iohk.cvp.cmanager.GroupsService/GetGroups',
  grpc.web.MethodType.UNARY,
  proto.io.iohk.cvp.cmanager.GetGroupsRequest,
  proto.io.iohk.cvp.cmanager.GetGroupsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetGroupsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetGroupsResponse.deserializeBinary
);


/**
 * @const
 * @type {!grpc.web.AbstractClientBase.MethodInfo<
 *   !proto.io.iohk.cvp.cmanager.GetGroupsRequest,
 *   !proto.io.iohk.cvp.cmanager.GetGroupsResponse>}
 */
const methodInfo_GroupsService_GetGroups = new grpc.web.AbstractClientBase.MethodInfo(
  proto.io.iohk.cvp.cmanager.GetGroupsResponse,
  /** @param {!proto.io.iohk.cvp.cmanager.GetGroupsRequest} request */
  function(request) {
    return request.serializeBinary();
  },
  proto.io.iohk.cvp.cmanager.GetGroupsResponse.deserializeBinary
);


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetGroupsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @param {function(?grpc.web.Error, ?proto.io.iohk.cvp.cmanager.GetGroupsResponse)}
 *     callback The callback function(error, response)
 * @return {!grpc.web.ClientReadableStream<!proto.io.iohk.cvp.cmanager.GetGroupsResponse>|undefined}
 *     The XHR Node Readable Stream
 */
proto.io.iohk.cvp.cmanager.GroupsServiceClient.prototype.getGroups =
    function(request, metadata, callback) {
  return this.client_.rpcCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.GroupsService/GetGroups',
      request,
      metadata || {},
      methodDescriptor_GroupsService_GetGroups,
      callback);
};


/**
 * @param {!proto.io.iohk.cvp.cmanager.GetGroupsRequest} request The
 *     request proto
 * @param {?Object<string, string>} metadata User defined
 *     call metadata
 * @return {!Promise<!proto.io.iohk.cvp.cmanager.GetGroupsResponse>}
 *     A native promise that resolves to the response
 */
proto.io.iohk.cvp.cmanager.GroupsServicePromiseClient.prototype.getGroups =
    function(request, metadata) {
  return this.client_.unaryCall(this.hostname_ +
      '/io.iohk.cvp.cmanager.GroupsService/GetGroups',
      request,
      metadata || {},
      methodDescriptor_GroupsService_GetGroups);
};


module.exports = proto.io.iohk.cvp.cmanager;

