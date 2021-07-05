package io.iohk.atala.prism.kycbridge.stubs

import monix.eval.Task

import io.iohk.atala.prism.kycbridge.models.identityMind.{ConsumerRequest, ConsumerResponse}
import io.iohk.atala.prism.kycbridge.services.IdentityMindService
import io.iohk.atala.prism.kycbridge.services.IdentityMindService.IdentityMindResponse

class IdentityMindServiceStub(
    consumerResponse: IdentityMindResponse[ConsumerResponse] = Right(
      ConsumerResponse(
        user = "",
        upr = None,
        frn = None,
        frp = None,
        frd = None,
        arpr = None
      )
    )
) extends IdentityMindService {

  override def consumer(
      consumerRequest: ConsumerRequest
  ): Task[IdentityMindResponse[ConsumerResponse]] =
    Task.pure(consumerResponse)

}
