package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._

case class AcuantFetchDocumentState1Data(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Array[Byte]
)

object AcuantFetchDocumentState1Data {
  implicit val acuantFetchDocumentState1DataEncoder: Encoder[AcuantFetchDocumentState1Data] = deriveEncoder
  implicit val acuantFetchDocumentState1DataDecoder: Decoder[AcuantFetchDocumentState1Data] = deriveDecoder
}
