package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.acas.AccessTokenResponseBody
import io.iohk.atala.prism.kycbridge.services.AcasService
import monix.eval.Task

class AcasServiceStub(getAccessTokenResponse: Either[Exception, AccessTokenResponseBody]) extends AcasService {

  def getAccessToken: Task[Either[Exception, AccessTokenResponseBody]] = Task.pure(getAccessTokenResponse)

}
