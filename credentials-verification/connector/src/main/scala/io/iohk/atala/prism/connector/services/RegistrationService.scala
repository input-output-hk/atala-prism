package io.iohk.atala.prism.connector.services

import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ParticipantId, ProtoCodecs, TransactionInfo}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.{node_api, node_models}

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
      did = DID(s"did:prism:${createDIDResponse.id}")
      transactionInfo = ProtoCodecs.fromTransactionInfo(
        createDIDResponse.transactionInfo.getOrElse(throw new RuntimeException("DID created has no transaction info"))
      )
      createRequest = ParticipantsRepository.CreateParticipantRequest(
        id = ParticipantId.random(),
        tpe = tpe,
        name = name,
        did = did,
        logo = logo,
        transactionInfo = transactionInfo
      )
      _ <- participantsRepository.create(createRequest)
    } yield RegistrationResult(
      did = did,
      id = createRequest.id,
      transactionInfo = transactionInfo
    )
  }
}

object RegistrationService {
  case class RegistrationResult(id: ParticipantId, did: DID, transactionInfo: TransactionInfo)
}
