package io.iohk.connector.services

import io.iohk.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.connector.repositories.ParticipantsRepository
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.nodenew.node_api.NodeServiceGrpc

import scala.concurrent.ExecutionContext

class RegistrationService(participantsRepository: ParticipantsRepository, nodeService: NodeServiceGrpc.NodeService)(
    implicit ec: ExecutionContext
) {

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      createDIDOperation: io.iohk.cvp.node_ops.SignedAtalaOperation
  ): FutureEither[Nothing, String] = {

    // TODO: Remove unneeded transformation by reusing the node protos
    val actualOp = io.iohk.nodenew.node_api.SignedAtalaOperation.parseFrom(createDIDOperation.toByteArray)
    for {
      createDIDResponse <- nodeService.createDID(actualOp).map(Right(_)).toFutureEither
      did = s"did:prism:${createDIDResponse.id}"
      createRequest = ParticipantsRepository.CreateParticipantRequest(
        id = ParticipantId.random(),
        tpe = tpe,
        name = name,
        did = did,
        logo = logo
      )
      _ <- participantsRepository.create(createRequest)
    } yield did
  }
}
