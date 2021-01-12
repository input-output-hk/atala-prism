package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.faceId.{Data, FaceMatchResponse}
import io.iohk.atala.prism.kycbridge.services.FaceIdService
import monix.eval.Task

class FaceIdServiceStub(faceMatchResponse: FaceMatchResponse = FaceMatchResponse(score = 100, isMatch = true))
    extends FaceIdService {

  def faceMatch(data: Data): Task[Either[Exception, FaceMatchResponse]] = Task.pure(Right(faceMatchResponse))
}
