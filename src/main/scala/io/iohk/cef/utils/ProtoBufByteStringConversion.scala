package io.iohk.cef.utils
import akka.util.ByteString

import scala.language.implicitConversions

object ProtoBufByteStringConversion {
  implicit def protoByteStringToAkkaByteString(bytes: com.google.protobuf.ByteString): ByteString =
    ByteString(bytes.toByteArray)

  implicit def akkaByteStringToProtoByteString(bytes: ByteString): com.google.protobuf.ByteString =
    com.google.protobuf.ByteString.copyFrom(bytes.toArray)
}
