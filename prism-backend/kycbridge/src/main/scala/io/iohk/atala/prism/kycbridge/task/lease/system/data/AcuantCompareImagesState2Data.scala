package io.iohk.atala.prism.kycbridge.task.lease.system.data

import io.circe._
import io.circe.generic.semiauto._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import io.iohk.atala.prism.kycbridge.models.assureId.Document

case class AcuantCompareImagesState2Data(
    receivedMessageId: String,
    connectionId: String,
    documentInstanceId: String,
    selfieImage: Array[Byte],
    document: Document
)

object AcuantCompareImagesState2Data {
  def fromAcuantFetchDocumentState1Data(
      data: AcuantFetchDocumentState1Data,
      document: Document
  ): AcuantCompareImagesState2Data = {
    AcuantCompareImagesState2Data(
      receivedMessageId = data.receivedMessageId,
      connectionId = data.connectionId,
      documentInstanceId = data.documentInstanceId,
      selfieImage = data.selfieImage,
      document = document
    )
  }

  implicit val acuantProcessingDataEncoder: Encoder[AcuantCompareImagesState2Data] = deriveEncoder
  implicit val acuantProcessingDataDecoder: Decoder[AcuantCompareImagesState2Data] = deriveDecoder
}
