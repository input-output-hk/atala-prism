package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.frontend.PlayJson
import io.iohk.cef.frontend.models._
import io.iohk.cef.ledger.chimeric._
import io.iohk.crypto._
import play.api.libs.json._

object Codecs
    extends PlayJson.Formats
    with CommonCodecs
    with ChimericCodecs
    with IdentityCodecs
    with DataItemCodecs
    with NetworkCodecs {

  implicit def signingKeyPairWrites(
      implicit pub: Writes[SigningPublicKey],
      priv: Writes[SigningPrivateKey]
  ): Writes[SigningKeyPair] = Writes { obj =>
    val map = Map(
      "publicKey" -> pub.writes(obj.public),
      "privateKey" -> priv.writes(obj.`private`)
    )

    JsObject(map)
  }

  implicit val UtxoResultFormat: Format[UtxoResult] = Json.format[UtxoResult]
  implicit val AddressResultFormat: Format[AddressResult] = Json.format[AddressResult]
  implicit val NonceResultFormat: Format[NonceResult] = Json.format[NonceResult]
  implicit val CurrencyQueryFormat: Format[CurrencyQuery] = Json.format[CurrencyQuery]
}
