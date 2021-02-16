package io.iohk.atala.cvp.webextension.background.wallet.models

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.prism.identity.DID

import scala.util.Try

case class WalletData(
    keys: Map[String, String],
    mnemonic: Mnemonic,
    organisationName: String,
    did: DID,
    transactionId: Option[String] = None,
    role: Role,
    logo: Array[Byte]
) {
  def addKey(name: String, key: String): WalletData = {
    copy(keys = keys + (name -> key))
  }
}

object WalletData {
  def fromJson(json: String): Try[WalletData] = {
    Try {
      println(s"Parsing wallet data from: $json")
      parse(json)
        .getOrElse(Json.obj())
        .as[WalletData]
        .getOrElse(throw new RuntimeException("Wallet could not be loaded from JSON"))
    }
  }
}
