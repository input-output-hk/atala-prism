package io.iohk.atala.prism.management.console.integrations

import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo, RegisterDID}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import io.iohk.atala.prism.models.{ProtoCodecs, TransactionInfo}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherFOps

import scala.concurrent.ExecutionContext

class ParticipantsIntegrationService(
    participantsRepository: ParticipantsRepository,
    nodeService: node_api.NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) {
  import ParticipantsIntegrationService._

  def register(request: RegisterDID): FutureEither[Nothing, RegistrationResult] = {
    for {
      createDIDResponse <-
        nodeService
          .createDID(node_api.CreateDIDRequest().withSignedOperation(request.signedOperation))
          .lift
      did = DID.buildPrismDID(createDIDResponse.id)
      transactionInfo = ProtoCodecs.fromTransactionInfo(
        createDIDResponse.transactionInfo.getOrElse(
          throw new RuntimeException("The DID created has no transaction info")
        )
      )
      createRequest = ParticipantsRepository.CreateParticipantRequest(
        id = ParticipantId.random(),
        name = request.name,
        did = did,
        logo = request.logo
      )
      _ <- participantsRepository.create(createRequest)
    } yield RegistrationResult(
      did = did,
      id = createRequest.id,
      transactionInfo = transactionInfo
    )
  }

  def getDetails(participantId: ParticipantId): FutureEither[errors.ManagementConsoleError, ParticipantInfo] = {
    participantsRepository.findBy(participantId)
  }
}
object ParticipantsIntegrationService {
  case class RegistrationResult(id: ParticipantId, did: DID, transactionInfo: TransactionInfo)
}
