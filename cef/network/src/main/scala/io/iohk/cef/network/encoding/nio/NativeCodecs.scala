package io.iohk.cef.network.encoding.nio

import java.nio.ByteBuffer
import java.nio.ByteBuffer.allocate
import java.nio.charset.StandardCharsets.UTF_8

import ByteLength._
import io.iohk.cef.network.encoding.nio.CodecDecorators.{typeCodeDecoder, typeCodeEncoder, verifyingRemaining}

import scala.reflect.ClassTag

trait NativeCodecs {

  implicit val stringEncoder: NioEncoder[String] = (s: String) => {
    val b = s.getBytes(UTF_8)
    val l = b.length
    allocate(4 + b.length).putInt(l).put(b).flip().asInstanceOf[ByteBuffer]
  }

  implicit val stringDecoder: NioDecoder[String] = (b: ByteBuffer) => {
    val l = b.getInt()
    val a = new Array[Byte](l)
    b.get(a)
    Option(new String(a, UTF_8))
  }

  implicit val booleanEncoder: NioEncoder[Boolean] = (b: Boolean) =>
    allocate(1).put(if (b) 1.toByte else 0.toByte).flip().asInstanceOf[ByteBuffer]

  implicit val booleanDecoder: NioDecoder[Boolean] = (b: ByteBuffer) => {
    Some(b.get() == 1.toByte)
  }

  implicit val byteEncoder: NioEncoder[Byte] = (b: Byte) => allocate(1).put(b).flip().asInstanceOf[ByteBuffer]

  implicit val byteDecoder: NioDecoder[Byte] = (b: ByteBuffer) => Some(b.get())

  implicit val shortEncoder: NioEncoder[Short] = (s: Short) =>
    allocate(lengthShort(s)).putShort(s).flip().asInstanceOf[ByteBuffer]

  implicit val shortDecoder: NioDecoder[Short] = (b: ByteBuffer) => Some(b.getShort())

  implicit val intEncoder: NioEncoder[Int] = (i: Int) => allocate(4).putInt(i).flip().asInstanceOf[ByteBuffer]

  implicit val intDecoder: NioDecoder[Int] = (b: ByteBuffer) => {
    Some(b.getInt())
  }

  implicit val longEncoder: NioEncoder[Long] = (l: Long) =>
    allocate(lengthLong(l)).putLong(l).flip().asInstanceOf[ByteBuffer]

  implicit val longDecoder: NioDecoder[Long] = (b: ByteBuffer) => Some(b.getLong)

  implicit val floatEncoder: NioEncoder[Float] = (f: Float) =>
    allocate(lengthFloat(f)).putFloat(f).flip().asInstanceOf[ByteBuffer]

  implicit val floatDecoder: NioDecoder[Float] = (b: ByteBuffer) => Some(b.getFloat)

  implicit val doubleEncoder: NioEncoder[Double] = (d: Double) =>
    allocate(lengthDouble(d)).putDouble(d).flip().asInstanceOf[ByteBuffer]

  implicit val doubleDecoder: NioDecoder[Double] = (b: ByteBuffer) => Some(b.getDouble())

  implicit val charEncoder: NioEncoder[Char] = (c: Char) =>
    allocate(lengthChar(c)).putChar(c).flip().asInstanceOf[ByteBuffer]

  implicit val charDecoder: NioDecoder[Char] = (b: ByteBuffer) => Some(b.getChar())

  implicit def arrayEncoder[T](implicit enc: NioEncoder[T],
                               lt: ByteLength[Array[T]],
                               ct: ClassTag[T]): NioEncoder[Array[T]] =
    typeCodeEncoder((a: Array[T]) => {

      val sizeBytes = lt(a)

      val accBuffer = ByteBuffer.allocate(sizeBytes).putInt(a.length)

      a.foldLeft(accBuffer)((accBuff, nextElmt) => accBuff.put(enc.encode(nextElmt)))
        .flip()
        .asInstanceOf[ByteBuffer]
    })

  implicit def arrayDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[Array[T]] =
    typeCodeDecoder((b: ByteBuffer) => {

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
    })

  def arrayEncoderImpl[T](implicit enc: NioEncoder[T], lt: ByteLength[Array[T]]): NioEncoder[Array[T]] =
    (a: Array[T]) => {

      val sizeBytes = lt(a)

      val accBuffer = ByteBuffer.allocate(sizeBytes).putInt(sizeBytes).putInt(a.length)

      a.foldLeft(accBuffer)((accBuff, nextElmt) => accBuff.put(enc.encode(nextElmt)))
        .flip()
        .asInstanceOf[ByteBuffer]
    }

  def arrayDecoderImpl[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[Array[T]] =
    (b: ByteBuffer) => {
      verifyingRemaining(4, b) {

        val sizeBytes = b.getInt()
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

//  implicit def iterableEncoder[T: ClassTag, I <: Iterable[T]](implicit enc: NioEncoder[T],
//                                                              lt: ByteLength[Array[T]]): NioEncoder[I] =
//    (i: I) => arrayEncoder(enc, lt).encode(i.toArray)
//
//
//  implicit def iterableDecoder[T, C](implicit dec: NioDecoder[T], cbf: CanBuildFrom[Array[T], T, C], ct: ClassTag[T]): NioDecoder[C] = (b: ByteBuffer) =>
//    arrayDecoder(dec, ct).decode(b).map(arr => cbf.apply(arr).result())

  implicit def listEncoder[T](implicit enc: NioEncoder[T],
                              lt: ByteLength[Array[T]],
                              ct: ClassTag[T]): NioEncoder[List[T]] =
    (l: List[T]) => arrayEncoder(enc, lt, ct).encode(l.toArray)

  implicit def listDecoder[T](implicit dec: NioDecoder[T], ct: ClassTag[T]): NioDecoder[List[T]] =
    (b: ByteBuffer) => arrayDecoder(dec, ct).decode(b).map(arr => List(arr: _*))
}

object NativeCodecs extends NativeCodecs
