package io.iohk.atala.prism.connector.services

import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.option._
import io.iohk.atala.prism.connector.errors.{ConnectorError, InvalidRequest}
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ParticipantId, ProtoCodecs, TransactionInfo}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, NodeServiceGrpc}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{common_models, node_api}

import scala.concurrent.{ExecutionContext, Future}

class RegistrationService(participantsRepository: ParticipantsRepository, nodeService: NodeServiceGrpc.NodeService)(
    implicit ec: ExecutionContext
) {

  import RegistrationService._

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      didOrOperation: Either[DID, SignedAtalaOperation]
  ): FutureEither[ConnectorError, RegistrationResult] = {

    for {
      createRequest <- didOrOperation.fold(checkAndUseExistingDID(_, tpe, name, logo), createDID(tpe, name, logo, _))
      _ <- participantsRepository.create(createRequest)
    } yield RegistrationResult(
      did = createRequest.did,
      id = createRequest.id,
      transactionInfo = createRequest.transactionInfo
    )
  }

  private def createDID(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      createDIDOperation: SignedAtalaOperation
  ): FutureEither[ConnectorError, ParticipantsRepository.CreateParticipantRequest] = {
    val result = for {
      createDIDResponse <- nodeService.createDID(node_api.CreateDIDRequest().withSignedOperation(createDIDOperation))
      did = DID.buildPrismDID(createDIDResponse.id)
      responseTransactionInfo <- createDIDResponse.transactionInfo.fold[Future[common_models.TransactionInfo]](
        Future.failed(new RuntimeException("DID created has no transaction info"))
      )(_.pure[Future])
      transactionInfo = ProtoCodecs.fromTransactionInfo(
        responseTransactionInfo
      )
      createRequest =
        ParticipantsRepository
          .CreateParticipantRequest(
            id = ParticipantId.random(),
            tpe = tpe,
            name = name,
            did = did,
            logo = logo,
            transactionInfo = transactionInfo.some
          )
          .asRight
    } yield createRequest
    result.toFutureEither
  }

  private def checkAndUseExistingDID(
      did: DID,
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo
  ): FutureEither[ConnectorError, ParticipantsRepository.CreateParticipantRequest] = {
    for {
      _ <-
        nodeService
          .getDidDocument(GetDidDocumentRequest(did.value))
          .map(_.document.toRight(InvalidRequest("the passed DID was not found on the node")))
          .toFutureEither
    } yield ParticipantsRepository.CreateParticipantRequest(
      id = ParticipantId.random(),
      tpe = tpe,
      name = name,
      did = did,
      logo = logo,
      transactionInfo = None
    )
  }
}

object RegistrationService {
  case class RegistrationResult(id: ParticipantId, did: DID, transactionInfo: Option[TransactionInfo])
}
