package io.iohk.cef.codecs.nio.components
import java.nio.ByteBuffer

import scala.annotation.tailrec
import scala.language.implicitConversions
import io.iohk.cef.codecs.nio.{NioCodec, NioStreamDecoder}

trait StreamCodecs {

  /**
    * Turn a standard decoder into StreamDecoder with decodeStream.
    */
  implicit def streamDecoderAdapter[T](codec: NioCodec[T]): NioStreamDecoder[T] = new NioStreamDecoder[T] {
    override def decodeStream(b: ByteBuffer): Seq[T] = {
      @annotation.tailrec
      def loop(acc: Seq[T]): Seq[T] = {
        codec.decode(b) match {
          case None => acc
          case Some(frame) => loop(acc :+ frame)
        }
      }
      loop(Vector())
    }
  }

  type ApplicableMessage = () => Unit
  type MessageApplication[Address] = (Address, ByteBuffer) => Seq[ApplicableMessage]

  def lazyMessageApplication[Address, Message, R](
      codec: NioCodec[Message],
      handler: (Address, Message) => Unit
  ): MessageApplication[Address] =
    (address, byteBuffer) => codec.decodeStream(byteBuffer).map(message => () => handler(address, message))

  def strictMessageApplication[Address, Message, R](
      codec: NioCodec[Message],
      handler: (Address, Message) => Unit
  ): MessageApplication[Address] =
    (address, byteBuffer) =>
      codec
        .decodeStream(byteBuffer)
        .map(message => {
          handler(address, message)
          () => ()
        })

  /**
    * decodeStream implements a 'decoder pipeline'
    * that will process a buffer containing multiple encoded objects.
    *
    * @param b the buffer to process
    * @param address the remote address of the inbound ByteBuffer.
    * @param messageAppliers These enable the application of message handlers with the decoded messages.
    *                        This can be done with strict application where handlers are called as soon
    *                        as the decoded message is available, or with lazy application where Function0
    *                        instances are handed back to the calling code for application.
    *
    *                        The contents of the buffer must be 'covered' by the decoders in application pipeline
    *                        i.e. if the buffer contains a sequence of bytes that none of the decoders
    *                        know how to handle, there is no choice but to give up.
    * @return a sequence of Function0 instances representing the bound Function2[Address, Message] message handlers.
    */
  def decodeStream[Address](
      address: Address,
      b: ByteBuffer,
      messageAppliers: Seq[MessageApplication[Address]]
  ): Seq[ApplicableMessage] = {

    @tailrec
    def bufferLoop(acc: Vector[ApplicableMessage]): Seq[ApplicableMessage] = {

      @tailrec
      def decoderLoop(iDecoder: Int, innerAcc: Vector[ApplicableMessage]): Seq[ApplicableMessage] = {
        if (iDecoder < messageAppliers.size) {
          val applicationResult: Seq[ApplicableMessage] = messageAppliers(iDecoder).apply(address, b)
          if (applicationResult.nonEmpty)
            innerAcc ++ applicationResult
          else
            decoderLoop(iDecoder + 1, innerAcc)
        } else
          innerAcc
      }

      val decodeResult = decoderLoop(0, Vector())

      if (decodeResult.isEmpty)
        // None of the decoders made progress.
        // Either processing is complete or they don't understand the buffer.
        acc
      else
        bufferLoop(acc ++ decodeResult)
    }

    bufferLoop(Vector())
  }

}

object StreamCodecs extends StreamCodecs
