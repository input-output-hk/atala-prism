package io.iohk.cef.ledger.chimeric

import scala.util.Try

case class TxOutRef(txId: ChimericTxId, index: Int) {
  override def toString(): String = s"$txId($index)"
}

object TxOutRef {

  /** Extracts a TxOutRef from a string of the form `someTxId(123)` */
  def parse(candidate: String): Option[TxOutRef] =
    for {
      (txId, indexCandidate) <- extractParts(candidate)
      index <- extractIndex(indexCandidate)
    } yield TxOutRef(txId, index)

  private def extractParts(candidate: String): Option[(String, String)] = {
    val regex = """([^\(]*)\((\d+)\)""".r
    candidate match {
      case regex(txId, indexCandidate) => Some((txId, indexCandidate))
      case _ => None
    }
  }

  private def extractIndex(indexCandidate: String): Option[Int] =
    Try(indexCandidate.toInt).toOption

}
