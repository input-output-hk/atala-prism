package io.iohk.cef.utils

import java.nio.ByteBuffer
import akka.util.ByteString
import com.google.protobuf.{ByteString => Protobuf}

trait BufferConversionOps {

  implicit class ByteBufferConversionOps(val byteBuffer: ByteBuffer) {
    def toArray: Array[Byte] = {
      if (byteBuffer.hasArray)
        byteBuffer.array
      else {
        byteBuffer.position(0)
        val arr = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(arr)
        arr
      }
    }
    def toByteString: ByteString = ByteString(toArray)
    def toProtobuf: Protobuf = Protobuf.copyFrom(toArray)
  }

  implicit class ByteStringConversionOps(val byteString: ByteString) {
    def toProtobuf: Protobuf = Protobuf.copyFrom(byteString.toArray)
  }

  implicit class ProtobufConversionOps(val protobuf: Protobuf) {
    def toArray: Array[Byte] = protobuf.toByteArray
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(protobuf.toByteArray)
    def toByteString: ByteString = ByteString(protobuf.toByteArray)
  }

  implicit class ArrayConversionOps(val array: Array[Byte]) {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(array)
    def toByteString: ByteString = ByteString(array)
    def toProtobuf: Protobuf = Protobuf.copyFrom(array)
  }

  implicit class ProtobufEntityConversionOps[T <: scalapb.GeneratedMessage](val entity: T) {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(entity.toByteArray)
    def toByteString: ByteString = ByteString(entity.toByteArray)
  }
}
