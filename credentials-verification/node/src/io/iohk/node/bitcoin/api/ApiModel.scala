package io.iohk.node.bitcoin
package api

import io.iohk.node.bitcoin.models._

object ApiModel {

  sealed abstract class BlockVerbosity(val int: Int)
  object BlockVerbosity {
    final case object Raw extends BlockVerbosity(1) // just block header expanded
    final case object Full extends BlockVerbosity(2) // block header and detailed transactions
  }

  sealed abstract class AddressType(val identifier: String)
  object AddressType {
    final case object Legacy extends AddressType("legacy")
    final case object P2shSegwit extends AddressType("p2sh-segwit")
    final case object Bech32 extends AddressType("bech32")
  }

  case class TransactionInput(txid: TransactionId, vout: Vout, sequence: Option[Long])
  case class TransactionOutput(data: Option[String], addresses: Map[Address, Btc]) {
    def asMap: Map[String, String] =
      (data.map(d => List(("data", d))).getOrElse(Nil) ++
        addresses.map { case (a, b) => (a: String, b.toString) }).toMap
  }
}
