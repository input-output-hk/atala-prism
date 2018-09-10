package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import io.iohk.cef.network.encoding.nio.CodecDecorators._
import io.iohk.cef.network.encoding.nio.ByteLength._

import scala.reflect.ClassTag

trait NativeCodecs {

  implicit val stringEncoder: NioEncoder[String] = (s: String) => {
    val byteLength = lengthString(s)
    val buffer = allocate(byteLength).putInt(s.length)
    s.foreach(buffer.putChar)
    buffer.flip().asInstanceOf[ByteBuffer]
  }

  implicit val stringDecoder: NioDecoder[String] = (b: ByteBuffer) => {
    val l = b.getInt()

    val a: Array[Char] = new Array[Char](l)

    Range(0, l).foreach(i => a(i) = b.getChar)

    Some(new String(a))
  }

  implicit val booleanEncoder: NioEncoder[Boolean] = (b: Boolean) =>
    allocate(lengthBoolean).put(if (b) 1.toByte else 0.toByte).flip().asInstanceOf[ByteBuffer]

  implicit val booleanDecoder: NioDecoder[Boolean] = (b: ByteBuffer) => {
    Some(b.get() == 1.toByte)
  }

  implicit val byteEncoder: NioEncoder[Byte] = (b: Byte) => allocate(lengthByte).put(b).flip().asInstanceOf[ByteBuffer]

  implicit val byteDecoder: NioDecoder[Byte] = (b: ByteBuffer) => Some(b.get())

  implicit val shortEncoder: NioEncoder[Short] = (s: Short) =>
    allocate(lengthShort).putShort(s).flip().asInstanceOf[ByteBuffer]

  implicit val shortDecoder: NioDecoder[Short] = (b: ByteBuffer) => Some(b.getShort())

  implicit val intEncoder: NioEncoder[Int] = (i: Int) => allocate(lengthInt).putInt(i).flip().asInstanceOf[ByteBuffer]

  implicit val intDecoder: NioDecoder[Int] = (b: ByteBuffer) => {
    Some(b.getInt())
  }

  implicit val longEncoder: NioEncoder[Long] = (l: Long) =>
    allocate(lengthLong).putLong(l).flip().asInstanceOf[ByteBuffer]

  implicit val longDecoder: NioDecoder[Long] = (b: ByteBuffer) => Some(b.getLong)

  implicit val floatEncoder: NioEncoder[Float] = (f: Float) =>
    allocate(lengthFloat).putFloat(f).flip().asInstanceOf[ByteBuffer]

  implicit val floatDecoder: NioDecoder[Float] = (b: ByteBuffer) => Some(b.getFloat)

  implicit val doubleEncoder: NioEncoder[Double] = (d: Double) =>
    allocate(lengthDouble).putDouble(d).flip().asInstanceOf[ByteBuffer]

  implicit val doubleDecoder: NioDecoder[Double] = (b: ByteBuffer) => Some(b.getDouble())

  implicit val charEncoder: NioEncoder[Char] = (c: Char) =>
    allocate(lengthChar).putChar(c).flip().asInstanceOf[ByteBuffer]

  implicit val charDecoder: NioDecoder[Char] = (b: ByteBuffer) => Some(b.getChar())

  implicit def arrayEncoder[T](implicit enc: NioEncoder[T], ct: ClassTag[T]): NioEncoder[Array[T]] =
    messageLengthEncoder(classCodeEncoder(arrayEncoderImpl))

  implicit def arrayDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[Array[T]] =
    messageLengthDecoder(classCodeDecoder(arrayDecoderImpl))

  def arrayEncoderImpl[T](implicit enc: NioEncoder[T]): NioEncoder[Array[T]] =
    (a: Array[T]) => {

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

      uberBuffer.flip().asInstanceOf[ByteBuffer]
    }

  def arrayDecoderImpl[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[Array[T]] =
    (b: ByteBuffer) => {
      val sizeElements = b.getInt()

      val arr = new Array[T](sizeElements)

      Range(0, sizeElements).foreach(i => {
        dec
          .decode(b)
          .foreach(decodedElement => {
            arr(i) = decodedElement
          })
      })

      Some(arr)
    }
}

object NativeCodecs extends NativeCodecs
