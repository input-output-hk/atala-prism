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
  def decodeStream(b: ByteBuffer, decoders: List[NioDecoder[_]]): Seq[Any] = {

    @tailrec
    def bufferLoop(acc: Vector[Any]): Vector[Any] = {

      @tailrec
      def decoderLoop(iDecoder: Int, innerAcc: Vector[Any]): Vector[Any] = {

        val messages = decoders(iDecoder).decodeStream(b)

        if (messages.isEmpty && iDecoder < decoders.size - 1) // decoder failed, try the next decoder
          decoderLoop(iDecoder + 1, innerAcc)
        else if (messages.nonEmpty) // decoder succeeded, return
          innerAcc ++ messages
        else // decoder failed but no more to try
          innerAcc
      }

      val messages = decoderLoop(0, Vector())

      if (messages.isEmpty)
        acc
      else
        bufferLoop(acc ++ messages)
    }

    bufferLoop(Vector())
  }
}

object StreamCodecs extends StreamCodecs
