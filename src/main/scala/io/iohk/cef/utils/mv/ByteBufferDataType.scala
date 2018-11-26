package io.iohk.cef.utils.mv
import java.nio.ByteBuffer

import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.`type`.DataType

import io.iohk.cef.codecs.nio._

class ByteBufferDataType[T](codec: NioEncDec[T]) extends DataType {

  override def compare(a: Any, b: Any): Int = {
    if (!a.isInstanceOf[Ordered[T]]) {
      throw new UnsupportedOperationException("Stored type is not orderable")
    }
    a.asInstanceOf[Ordered[T]].compare(b.asInstanceOf[T])
  }

  override def getMemory(obj: Any): Int =
    codec.encode(obj.asInstanceOf[T]).capacity()

  override def write(buff: WriteBuffer, obj: Any): Unit =
    buff.put(codec.encode(obj.asInstanceOf[T]))

  override def write(buff: WriteBuffer, obj: Array[AnyRef], len: Int, key: Boolean): Unit = {
    require(!key)
    (0 until len).foreach(i => write(buff, obj(i)))
  }

  override def read(buff: ByteBuffer): AnyRef =
    codec
      .decode(buff)
      .getOrElse(throw new IllegalStateException("Decoding error in underlying storage"))
      .asInstanceOf[AnyRef]

  override def read(buff: ByteBuffer, obj: Array[AnyRef], len: Int, key: Boolean): Unit = {
    require(!key)
    (0 until len).foreach(i => obj(i) = read(buff))
  }
}
