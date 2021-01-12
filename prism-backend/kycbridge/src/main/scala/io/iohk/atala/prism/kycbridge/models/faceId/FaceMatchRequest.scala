package io.iohk.atala.prism.kycbridge.models.faceId

import java.util.Base64

case class Data(imageOne: String, imageTwo: String)

object Data {
  def apply(imageOne: Array[Byte], imageTwo: Array[Byte]): Data = {
    Data(
      imageOne = Base64.getEncoder.encodeToString(imageOne),
      imageTwo = Base64.getEncoder.encodeToString(imageTwo)
    )
  }
}

case class Settings(subscriptionId: String)

case class FaceMatchRequest(data: Data, settings: Settings)
