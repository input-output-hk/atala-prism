package io.iohk.node.cardano.wallet.api

import com.softwaremill.sttp.Method
import io.circe.syntax._
import io.circe.{Encoder, Json}
import io.iohk.node.cardano.models.{Payment, WalletId}

import io.iohk.node.cardano.wallet.api.JsonCodecs._

import scala.language.implicitConversions

private sealed abstract class ApiRequest(val path: String, val httpMethod: Method) extends Product with Serializable {
  def requestBody: Json

  protected final implicit def jsonView[T](t: T)(implicit e: Encoder[T]): Json =
    t.asJson
}

private[api] object ApiRequest {

  final case class PostTransaction(
      walletId: WalletId,
      payments: List[Payment],
      passphrase: String
  ) extends ApiRequest(s"v2/byron-wallets/$walletId/transactions", Method.POST) {
    override def requestBody: Json = Json.obj(("payments", payments), ("passphrase", passphrase))
  }
}
