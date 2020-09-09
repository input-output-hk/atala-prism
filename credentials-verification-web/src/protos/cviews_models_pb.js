/* eslint-disable */
// source: cviews_models.proto
/**
 * @fileoverview
 * @enhanceable
 * @suppress {messageConventions} JS Compiler reports an error if a variable or
 *     field starts with 'MSG_' and isn't a translatable message.
 * @public
 */
// GENERATED CODE -- DO NOT EDIT!

var jspb = require('google-protobuf');
var goog = jspb;
var global = Function('return this')();

goog.exportSymbol('proto.io.iohk.atala.prism.protos.CredentialViewTemplate', null, global);
/**
 * Generated by JsPbCodeGenerator.
 * @param {Array=} opt_data Optional initial data array, typically from a
 * server response, or constructed directly in Javascript. The array is used
 * in place and becomes part of the constructed object. It is not cloned.
 * If no data is provided, the constructed object will be empty, but still
 * valid.
 * @extends {jspb.Message}
 * @constructor
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate = function(opt_data) {
  jspb.Message.initialize(this, opt_data, 0, -1, null, null);
};
goog.inherits(proto.io.iohk.atala.prism.protos.CredentialViewTemplate, jspb.Message);
if (goog.DEBUG && !COMPILED) {
  /**
   * @public
   * @override
   */
  proto.io.iohk.atala.prism.protos.CredentialViewTemplate.displayName = 'proto.io.iohk.atala.prism.protos.CredentialViewTemplate';
}



if (jspb.Message.GENERATE_TO_OBJECT) {
/**
 * Creates an object representation of this proto.
 * Field names that are reserved in JavaScript and will be renamed to pb_name.
 * Optional fields that are not set will be set to undefined.
 * To access a reserved field use, foo.pb_<name>, eg, foo.pb_default.
 * For the list of reserved names please see:
 *     net/proto2/compiler/js/internal/generator.cc#kKeyword.
 * @param {boolean=} opt_includeInstance Deprecated. whether to include the
 *     JSPB instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @return {!Object}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.toObject = function(opt_includeInstance) {
  return proto.io.iohk.atala.prism.protos.CredentialViewTemplate.toObject(opt_includeInstance, this);
};


/**
 * Static version of the {@see toObject} method.
 * @param {boolean|undefined} includeInstance Deprecated. Whether to include
 *     the JSPB instance for transitional soy proto support:
 *     http://goto/soy-param-migration
 * @param {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} msg The msg instance to transform.
 * @return {!Object}
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.toObject = function(includeInstance, msg) {
  var f, obj = {
    id: jspb.Message.getFieldWithDefault(msg, 1, 0),
    name: jspb.Message.getFieldWithDefault(msg, 2, ""),
    encodedlogoimage: jspb.Message.getFieldWithDefault(msg, 3, ""),
    logoimagemimetype: jspb.Message.getFieldWithDefault(msg, 4, ""),
    htmltemplate: jspb.Message.getFieldWithDefault(msg, 5, "")
  };

  if (includeInstance) {
    obj.$jspbMessageInstance = msg;
  }
  return obj;
};
}


/**
 * Deserializes binary data (in protobuf wire format).
 * @param {jspb.ByteSource} bytes The bytes to deserialize.
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.deserializeBinary = function(bytes) {
  var reader = new jspb.BinaryReader(bytes);
  var msg = new proto.io.iohk.atala.prism.protos.CredentialViewTemplate;
  return proto.io.iohk.atala.prism.protos.CredentialViewTemplate.deserializeBinaryFromReader(msg, reader);
};


/**
 * Deserializes binary data (in protobuf wire format) from the
 * given reader into the given message object.
 * @param {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} msg The message object to deserialize into.
 * @param {!jspb.BinaryReader} reader The BinaryReader to use.
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.deserializeBinaryFromReader = function(msg, reader) {
  while (reader.nextField()) {
    if (reader.isEndGroup()) {
      break;
    }
    var field = reader.getFieldNumber();
    switch (field) {
    case 1:
      var value = /** @type {number} */ (reader.readInt64());
      msg.setId(value);
      break;
    case 2:
      var value = /** @type {string} */ (reader.readString());
      msg.setName(value);
      break;
    case 3:
      var value = /** @type {string} */ (reader.readString());
      msg.setEncodedlogoimage(value);
      break;
    case 4:
      var value = /** @type {string} */ (reader.readString());
      msg.setLogoimagemimetype(value);
      break;
    case 5:
      var value = /** @type {string} */ (reader.readString());
      msg.setHtmltemplate(value);
      break;
    default:
      reader.skipField();
      break;
    }
  }
  return msg;
};


/**
 * Serializes the message to binary data (in protobuf wire format).
 * @return {!Uint8Array}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.serializeBinary = function() {
  var writer = new jspb.BinaryWriter();
  proto.io.iohk.atala.prism.protos.CredentialViewTemplate.serializeBinaryToWriter(this, writer);
  return writer.getResultBuffer();
};


/**
 * Serializes the given message to binary data (in protobuf wire
 * format), writing to the given BinaryWriter.
 * @param {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} message
 * @param {!jspb.BinaryWriter} writer
 * @suppress {unusedLocalVariables} f is only used for nested messages
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.serializeBinaryToWriter = function(message, writer) {
  var f = undefined;
  f = message.getId();
  if (f !== 0) {
    writer.writeInt64(
      1,
      f
    );
  }
  f = message.getName();
  if (f.length > 0) {
    writer.writeString(
      2,
      f
    );
  }
  f = message.getEncodedlogoimage();
  if (f.length > 0) {
    writer.writeString(
      3,
      f
    );
  }
  f = message.getLogoimagemimetype();
  if (f.length > 0) {
    writer.writeString(
      4,
      f
    );
  }
  f = message.getHtmltemplate();
  if (f.length > 0) {
    writer.writeString(
      5,
      f
    );
  }
};


/**
 * optional int64 id = 1;
 * @return {number}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.getId = function() {
  return /** @type {number} */ (jspb.Message.getFieldWithDefault(this, 1, 0));
};


/**
 * @param {number} value
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} returns this
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.setId = function(value) {
  return jspb.Message.setProto3IntField(this, 1, value);
};


/**
 * optional string name = 2;
 * @return {string}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.getName = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 2, ""));
};


/**
 * @param {string} value
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} returns this
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.setName = function(value) {
  return jspb.Message.setProto3StringField(this, 2, value);
};


/**
 * optional string encodedLogoImage = 3;
 * @return {string}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.getEncodedlogoimage = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 3, ""));
};


/**
 * @param {string} value
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} returns this
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.setEncodedlogoimage = function(value) {
  return jspb.Message.setProto3StringField(this, 3, value);
};


/**
 * optional string logoImageMimeType = 4;
 * @return {string}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.getLogoimagemimetype = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 4, ""));
};


/**
 * @param {string} value
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} returns this
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.setLogoimagemimetype = function(value) {
  return jspb.Message.setProto3StringField(this, 4, value);
};


/**
 * optional string htmlTemplate = 5;
 * @return {string}
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.getHtmltemplate = function() {
  return /** @type {string} */ (jspb.Message.getFieldWithDefault(this, 5, ""));
};


/**
 * @param {string} value
 * @return {!proto.io.iohk.atala.prism.protos.CredentialViewTemplate} returns this
 */
proto.io.iohk.atala.prism.protos.CredentialViewTemplate.prototype.setHtmltemplate = function(value) {
  return jspb.Message.setProto3StringField(this, 5, value);
};


goog.object.extend(exports, proto.io.iohk.atala.prism.protos);
