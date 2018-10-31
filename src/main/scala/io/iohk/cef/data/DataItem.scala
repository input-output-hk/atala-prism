package io.iohk.cef.data
import java.nio.ByteBuffer

import io.iohk.cef.crypto._

trait DataItem[T] {

  def data: T

  def id: String

  /**
    * Users/entities that witnessed this item and signed it
    *
    * @return
    */
  def witnesses: Seq[Witness]

  /**
    * Users/entities with permission to eliminate this data item
    *
    * @return
    */
  //TODO we will need to replace Seq with a simple boolean AST to better express ownership
  def owners: Seq[Owner]

  /**
    * Validates the data item and returns a specific error or nothing.
    * Does it make sense to return something else?
    *
    * @return
    */
  def apply(): Either[DataItemError, Unit]
}

object DataItem {
  import io.iohk.cef.codecs.nio.{NioEncoder, NioDecoder, NioEncDec}

  // TOOD enable automatic codec derivation.
  implicit def dataItemNioEncoder[T]: NioEncoder[DataItem[T]] = (dataItem: DataItem[T]) => ???

  implicit def dataItemNioDecoder[T]: NioDecoder[DataItem[T]] = (b: ByteBuffer) => ???

  implicit def nioCodec[T]: NioEncDec[DataItem[T]] = new NioEncDec[DataItem[T]] {
    override def encode(dataItem: DataItem[T]): ByteBuffer = dataItemNioEncoder[T].encode(dataItem)
    override def decode(b: ByteBuffer): Option[DataItem[T]] = dataItemNioDecoder[T].decode(b)
  }

  def validate[I](dataItem: DataItem[I])(implicit enc: NioEncoder[I]): Boolean =
    validateSignatures(dataItem).forall(_._2)

  def validateSignatures[I](dataItem: DataItem[I])(implicit enc: CryptoEncoder[I]): Seq[(Signature, Boolean)] = {
    dataItem.witnesses.map {
      case Witness(key, signature) =>
        (signature, isValidSignature(dataItem, signature, key))
    }
  }
}
