package io.iohk.cef.codecs.nio.components
import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import io.iohk.cef.codecs.nio.components.ByteLength.lengthInt
import io.iohk.cef.codecs.nio.components.CodecDecorators.verifyingRemaining
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.components.Ops._

import scala.reflect.runtime.universe.TypeTag

private[components] object NativeCodecHelpers {

  def nativeDecoder[T: TypeTag](size: Int, extract: ByteBuffer => T): NioDecoder[T] =
    NioDecoder((b: ByteBuffer) => verifyingRemaining(size, b) { Some(extract(b)) })

  def nativeEncoder[T: TypeTag](size: Int, put: ByteBuffer => T => ByteBuffer): NioEncoder[T] =
    NioEncoder((t: T) => new ByteBufferExtension(put(allocate(size))(t)).back())

  def nativeArrayEncoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      put: TB => Array[T] => TB): NioEncoder[Array[T]] =
    untaggedNativeArrayEncoder(tSize, as)(put).packed

  def untaggedNativeArrayEncoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      put: TB => Array[T] => TB): NioEncoder[Array[T]] = {
    val inner =
      (sa: Array[T]) => {
        val size = 4 + sa.length * tSize
        val b = ByteBuffer.allocate(size)
        b.putInt(sa.length)
        put(as(b))(sa)
        b.back()
      }
    NioEncoder(inner)
  }

  def nativeArrayDecoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      get: TB => Array[T] => TB): NioDecoder[Array[T]] =
    untaggedNativeArrayDecoder(tSize, as)(get).packed

  def untaggedNativeArrayDecoder[T: TypeTag, TB](tSize: Int, as: ByteBuffer => TB)(
      get: TB => Array[T] => TB): NioDecoder[Array[T]] = {
    val inner = { b: ByteBuffer =>
      verifyingRemaining(lengthInt, b) {
        val arrayLength = b.getInt
        val arraySize = arrayLength * tSize
        verifyingRemaining(arraySize, b) {
          val r = new Array[T](arrayLength)
          val oldPosition: Int = b.position
          get(as(b))(r)
          b.position(oldPosition + arraySize)
          Some(r)
        }
      }
    }
    NioDecoder(inner)
  }

  def variableSizeArrayEncoderImpl[T](implicit enc: NioEncoder[T]): NioEncoder[Array[T]] = {
    implicit val ttT: TypeTag[T] = enc.typeTag
    NioEncoder((a: Array[T]) => {

      val acc = (Vector[ByteBuffer](), 0)

      val (buffers, totalSize) = a.foldLeft(acc)((currentAcc, nextElmt) => {
        val byteBuffer = enc.encode(nextElmt)
        currentAcc match {
          case (bufferStorage, size) =>
            (bufferStorage :+ byteBuffer, size + byteBuffer.remaining())
        }
      })

      // buffer for the size metadata and all elements.
      val uberBuffer = ByteBuffer.allocate(totalSize + 4).putInt(a.length)

      buffers.foreach(buffer => uberBuffer.put(buffer))

      uberBuffer.back()
    })
  }

  def variableSizeArrayDecoderImpl[T](implicit dec: NioDecoder[T]): NioDecoder[Array[T]] = {
    implicit val ttT: TypeTag[T] = dec.typeTag
    NioDecoder((b: ByteBuffer) => {
      verifyingRemaining(4, b) {
        val sizeElements = b.getInt()
        val arr = new Array[T](sizeElements)

        var i = 0
        var r: Option[Array[T]] = Some(arr)
        while (i < sizeElements && r.isDefined) {
          dec.decode(b) match {
            case None => r = None
            case Some(e) => arr(i) = e
          }
          i += 1
        }

        r
      }
    })
  }
}
