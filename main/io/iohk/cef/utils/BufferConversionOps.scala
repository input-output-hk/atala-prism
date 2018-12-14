package io.iohk.cef.utils

import java.nio.ByteBuffer
import akka.util.ByteString

trait BufferConversionOps {

  implicit class ByteBufferConversionOps(val byteBuffer: ByteBuffer) {
    def toArray: Array[Byte] = {
      if (byteBuffer.hasArray)
        byteBuffer.array
      else {
        (byteBuffer: java.nio.Buffer).position(0)
        val arr = new Array[Byte](byteBuffer.remaining())
        byteBuffer.get(arr)
        arr
      }
    }
    def toByteString: ByteString = ByteString(toArray)
  }

  implicit class ArrayConversionOps(val array: Array[Byte]) {
    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(array)
    def toByteString: ByteString = ByteString(array)
  }
}
