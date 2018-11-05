package io.iohk.cef.ledger

import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

case class Block[S, Header <: BlockHeader, Tx <: Transaction[S]](header: Header, transactions: Seq[Tx])
    extends (LedgerState[S] => Either[LedgerError, LedgerState[S]]) {

  override def apply(state: LedgerState[S]): Either[LedgerError, LedgerState[S]] = {
    transactions.foldLeft[Either[LedgerError, LedgerState[S]]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }

  /**
    * See the doc in Transaction
    */
  def partitionIds: Set[String] = {
    transactions.foldLeft[Set[String]](Set())(_ ++ _.partitionIds)
  }

  override def toString(): String = s"Block($header,$transactions)"
}
object Block {
  implicit def BlockEncDec[S, Header <: BlockHeader, Tx <: Transaction[S]](
      implicit s: NioEncDec[S],
      h: NioEncDec[Header],
      t: NioEncDec[Tx]): NioEncDec[Block[S, Header, Tx]] = {
    import io.iohk.cef.codecs.nio.auto._
    implicit val tts: TypeTag[S] = s.typeTag
    implicit val tth: TypeTag[Header] = h.typeTag
    implicit val ttt: TypeTag[Tx] = t.typeTag
    val e: NioEncoder[Block[S, Header, Tx]] = genericEncoder
    val d: NioDecoder[Block[S, Header, Tx]] = genericDecoder
    NioEncDec(e, d)
  }
}
