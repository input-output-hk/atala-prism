package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.acas.AccessTokenResponseBody
import io.iohk.atala.prism.kycbridge.services.AcasService
import io.iohk.atala.prism.kycbridge.services.AcasService.AcasServiceError
import monix.eval.Task

class AcasServiceStub(getAccessTokenResponse: Either[AcasServiceError, AccessTokenResponseBody]) extends AcasService {

  def getAccessToken: Task[Either[AcasServiceError, AccessTokenResponseBody]] = Task.pure(getAccessTokenResponse)

}
