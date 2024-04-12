package io.iohk.atala.prism.node.cardano.wallet.api
import io.circe.syntax._
import io.circe.{Encoder, Json}
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.cardano.models.{Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.JsonCodecs._
import sttp.model.Method

private sealed abstract class ApiRequest(
    val path: String,
    val httpMethod: Method
) extends Product
    with Serializable {
  def requestBody: Option[Json]

  protected final implicit def jsonView[T](t: T)(implicit e: Encoder[T]): Json =
    t.asJson
}

private[api] object ApiRequest {

  final case class EstimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ) extends ApiRequest(s"v2/wallets/$walletId/payment-fees", Method.POST) {
    override def requestBody: Option[Json] = {
      Some(
        Json.fromFields(
          asJsonFields("payments" -> payments) ++ asJsonField(metadata)
        )
      )
    }
  }

  final case class PostTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ) extends ApiRequest(s"v2/wallets/$walletId/transactions", Method.POST) {
    override def requestBody: Option[Json] = {
      Some(
        Json.fromFields(
          asJsonFields(
            "payments" -> payments,
            "passphrase" -> passphrase
          ) ++ asJsonField(metadata)
        )
      )
    }
  }

  final case class GetTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ) extends ApiRequest(
        s"v2/wallets/$walletId/transactions/$transactionId",
        Method.GET
      ) {
    override def requestBody: Option[Json] = None
  }

  final case class DeleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ) extends ApiRequest(
        s"v2/wallets/$walletId/transactions/$transactionId",
        Method.DELETE
      ) {
    override def requestBody: Option[Json] = None
  }

  final case class GetWallet(walletId: WalletId) extends ApiRequest(s"v2/wallets/$walletId", Method.GET) {
    override def requestBody: Option[Json] = None
  }

  private def asJsonFields(fields: (String, Json)*): Array[(String, Json)] = {
    Array(fields: _*)
  }

  private def asJsonField(
      metadata: Option[TransactionMetadata]
  ): Array[(String, Json)] = {
    metadata
      .map(_.json)
      .fold(Array[(String, Json)]())(meta => Array(("metadata", meta)))
  }
}
