package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate

import akka.util.ByteString
import io.iohk.cef.network.encoding.nio.CodecDecorators._

import scala.reflect.ClassTag

trait NativeCodecs {

  implicit val stringEncoder: NioEncoder[String] = (s: String) => {
    val byteLength = 4 + s.length * 2
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
    allocate(1).put(if (b) 1.toByte else 0.toByte).flip().asInstanceOf[ByteBuffer]

  implicit val booleanDecoder: NioDecoder[Boolean] = (b: ByteBuffer) => {
    Some(b.get() == 1.toByte)
  }

  implicit val byteEncoder: NioEncoder[Byte] = (b: Byte) => allocate(1).put(b).flip().asInstanceOf[ByteBuffer]

  implicit val byteDecoder: NioDecoder[Byte] = (b: ByteBuffer) => Some(b.get())

  implicit val shortEncoder: NioEncoder[Short] = (s: Short) =>
    allocate(2).putShort(s).flip().asInstanceOf[ByteBuffer]

  implicit val shortDecoder: NioDecoder[Short] = (b: ByteBuffer) => Some(b.getShort())

  implicit val intEncoder: NioEncoder[Int] = (i: Int) => allocate(4).putInt(i).flip().asInstanceOf[ByteBuffer]

  implicit val intDecoder: NioDecoder[Int] = (b: ByteBuffer) => {
    Some(b.getInt())
  }

  implicit val longEncoder: NioEncoder[Long] = (l: Long) =>
    allocate(8).putLong(l).flip().asInstanceOf[ByteBuffer]

  implicit val longDecoder: NioDecoder[Long] = (b: ByteBuffer) => Some(b.getLong)

  implicit val floatEncoder: NioEncoder[Float] = (f: Float) =>
    allocate(4).putFloat(f).flip().asInstanceOf[ByteBuffer]

  implicit val floatDecoder: NioDecoder[Float] = (b: ByteBuffer) => Some(b.getFloat)

  implicit val doubleEncoder: NioEncoder[Double] = (d: Double) =>
    allocate(8).putDouble(d).flip().asInstanceOf[ByteBuffer]

  implicit val doubleDecoder: NioDecoder[Double] = (b: ByteBuffer) => Some(b.getDouble())

  implicit val charEncoder: NioEncoder[Char] = (c: Char) =>
    allocate(2).putChar(c).flip().asInstanceOf[ByteBuffer]

  implicit val charDecoder: NioDecoder[Char] = (b: ByteBuffer) => Some(b.getChar())

  implicit def arrayEncoder[T](implicit enc: NioEncoder[T],
                               ct: ClassTag[T]): NioEncoder[Array[T]] =
    messageLengthEncoder(typeCodeEncoder(arrayEncoderImpl))

  implicit def arrayDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[Array[T]] =
    messageLengthDecoder(typeCodeDecoder(arrayDecoderImpl))

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

  implicit def listEncoder[T](implicit enc: NioEncoder[T],
                              ct: ClassTag[T]): NioEncoder[List[T]] =
    (l: List[T]) => arrayEncoder(enc, ct).encode(l.toArray)

  implicit def listDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[List[T]] =
    (b: ByteBuffer) => arrayDecoder(dec, ct).decode(b).map(arr => List(arr: _*))

  implicit val byteStringEncoder: NioEncoder[ByteString] = new NioEncoder[ByteString] {
    override def encode(b: ByteString): ByteBuffer =
      arrayEncoder(byteEncoder, ClassTag(classOf[Byte])).encode(b.toArray)
  }

  implicit val byteStringDecoder: NioDecoder[ByteString] = new NioDecoder[ByteString] {
    override def decode(b: ByteBuffer): Option[ByteString] =
      arrayDecoder(byteDecoder, ClassTag(classOf[Byte])).decode(b).map(arr => ByteString(arr))
  }
}

object NativeCodecs extends NativeCodecs
