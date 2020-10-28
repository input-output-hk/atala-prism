package io.iohk.atala.prism.node.cardano.wallet.api

import com.softwaremill.sttp.Method
import io.circe.syntax._
import io.circe.{Encoder, Json}
import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.node.cardano.models.{Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.JsonCodecs._

private sealed abstract class ApiRequest(val path: String, val httpMethod: Method) extends Product with Serializable {
  def requestBody: Option[Json]

  protected final implicit def jsonView[T](t: T)(implicit e: Encoder[T]): Json =
    t.asJson
}

private[api] object ApiRequest {

  final case class PostTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ) extends ApiRequest(s"v2/wallets/$walletId/transactions", Method.POST) {
    override def requestBody: Option[Json] = {
      val metadataFields = metadata.map(_.json).fold(Array[(String, Json)]())(meta => Array(("metadata", meta)))
      val fields = Array[(String, Json)](("payments", payments), ("passphrase", passphrase)) ++ metadataFields
      Some(Json.obj(fields.toIndexedSeq: _*))
    }
  }

  final case class GetTransaction(walletId: WalletId, transactionId: TransactionId)
      extends ApiRequest(s"v2/wallets/$walletId/transactions/$transactionId", Method.GET) {
    override def requestBody: Option[Json] = None
  }

  final case class DeleteTransaction(walletId: WalletId, transactionId: TransactionId)
      extends ApiRequest(s"v2/wallets/$walletId/transactions/$transactionId", Method.DELETE) {
    override def requestBody: Option[Json] = None
  }
}
