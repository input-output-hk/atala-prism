package io.iohk.cef.network.encoding.nio
import java.nio.ByteBuffer

import scala.annotation.tailrec
import scala.language.implicitConversions

trait StreamCodecs {

  /**
    * Turn a standard decoder into StreamDecoder with decodeStream.
    */
  implicit def streamDecoderAdapter[T](dec: NioDecoder[T]): NioStreamDecoder[T] = new NioStreamDecoder[T] {
    override def decodeStream(b: ByteBuffer): Seq[T] = {
      @annotation.tailrec
      def loop(acc: Seq[T]): Seq[T] = {
        dec.decode(b) match {
          case None        => acc
          case Some(frame) => loop(acc :+ frame)
        }
      }
      loop(Vector())
    }
  }

  /**
    * decodeStream implements a 'decoder pipeline'
    * that will process a buffer containing multiple encoded objects.
    *
    * @param b the buffer to process
    * @param decoders the decoder chain. Note that the types of encoded objects
    *                 in the buffer must be 'covered' by the decoders. i.e. if
    *                 the buffer contains a sequence of bytes that none of the decoders
    *                 know how to handle, there is no choice but to give up.
    * @return a sequence of decoded objects.
    */
  type DecoderFunction2[Address] = (Address, ByteBuffer) => Boolean

  def decoderFunction2[Address, Message](decoder: NioDecoder[Message],
                                         handler: (Address, Message) => Unit): DecoderFunction2[Address] =
    (address, byteBuffer) => {
      val messages = decoder.decodeStream(byteBuffer)
      messages.foreach(message => handler(address, message))
      messages.nonEmpty
    }

  def decodeStream[Address](address: Address, b: ByteBuffer, decoderHandlers: Seq[DecoderFunction2[Address]]): Unit = {

    @tailrec
    def bufferLoop(): Unit = {

      @tailrec
      def decoderLoop(iDecoder: Int): Boolean = {
        if (iDecoder < decoderHandlers.size)
          if (decoderHandlers(iDecoder).apply(address, b))
            true
          else
            decoderLoop(iDecoder + 1)
        else
          false
      }

      if (decoderLoop(0))
        bufferLoop()
      else
        ()
    }

    bufferLoop()
  }

}

object StreamCodecs extends StreamCodecs
