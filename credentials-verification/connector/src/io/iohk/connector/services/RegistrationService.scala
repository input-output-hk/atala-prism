package io.iohk.connector.services

import io.iohk.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.ParticipantsRepository
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.prism.protos.node_api.NodeServiceGrpc
import io.iohk.prism.protos.{node_api, node_models}

import scala.concurrent.ExecutionContext

class RegistrationService(participantsRepository: ParticipantsRepository, nodeService: NodeServiceGrpc.NodeService)(
    implicit ec: ExecutionContext
) {

  import RegistrationService._

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      createDIDOperation: node_models.SignedAtalaOperation
  ): FutureEither[Nothing, RegistrationResult] = {

    for {
      createDIDResponse <-
        nodeService
          .createDID(node_api.CreateDIDRequest().withSignedOperation(createDIDOperation))
          .map(Right(_))
          .toFutureEither
      did = s"did:prism:${createDIDResponse.id}"
      createRequest = ParticipantsRepository.CreateParticipantRequest(
        id = ParticipantId.random(),
        tpe = tpe,
        name = name,
        did = did,
        logo = logo
      )
      _ <- participantsRepository.create(createRequest)
    } yield RegistrationResult(did = did, id = createRequest.id)
  }
}

object RegistrationService {
  case class RegistrationResult(id: ParticipantId, did: String)
}
