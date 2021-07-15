package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.utils.Base64ByteArrayWrapper

case class SendForAcuantManualReviewReadyStateData(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Base64ByteArrayWrapper,
    mtid: String
)

object SendForAcuantManualReviewReadyStateData {
  implicit val sendForAcuantManualReviewReadyStateDataEncoder: Encoder[SendForAcuantManualReviewReadyStateData] =
    deriveEncoder
  implicit val sendForAcuantManualReviewReadyStateDataDecoder: Decoder[SendForAcuantManualReviewReadyStateData] =
    deriveDecoder
}
