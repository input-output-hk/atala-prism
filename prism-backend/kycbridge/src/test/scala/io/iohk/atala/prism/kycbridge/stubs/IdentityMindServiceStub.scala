package io.iohk.atala.prism.kycbridge.stubs

import monix.eval.Task

import io.iohk.atala.prism.kycbridge.models.identityMind._
import io.iohk.atala.prism.kycbridge.services.IdentityMindService
import io.iohk.atala.prism.kycbridge.services.IdentityMindService.IdentityMindResponse
import io.iohk.atala.prism.kycbridge.models.identityMind.{AttributesRequest, AttributesResponse}

class IdentityMindServiceStub(
    postConsumerResponse: IdentityMindResponse[PostConsumerResponse] = Right(
      PostConsumerResponse(
        mtid = "mtid",
        user = "",
        upr = None,
        frn = None,
        frp = None,
        frd = None,
        arpr = None
      )
    ),
    getConsumerResponse: IdentityMindResponse[GetConsumerResponse] = Right(
      GetConsumerResponse(
        mtid = "mtid",
        state = ConsumerResponseState.Accept,
        ednaScoreCard = EdnaScoreCard(etr = Nil)
      )
    ),
    attributesResponse: IdentityMindResponse[AttributesResponse] = Right(
      AttributesResponse(
        progress = "DONE"
      )
    )
) extends IdentityMindService {

  override def consumer(
      consumerRequest: PostConsumerRequest
  ): Task[IdentityMindResponse[PostConsumerResponse]] =
    Task.pure(postConsumerResponse)

  override def consumer(consumerRequest: GetConsumerRequest): Task[IdentityMindResponse[GetConsumerResponse]] =
    Task.pure(getConsumerResponse)

  override def attributes(attributesRequest: AttributesRequest): Task[IdentityMindResponse[AttributesResponse]] =
    Task.pure(attributesResponse)

}
