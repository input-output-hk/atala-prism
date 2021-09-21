package io.iohk.atala.prism.connector.services

import cats.effect.{BracketThrow, MonadThrow}
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.option._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.connector.errors.{ConnectorError, InvalidRequest}
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.connector.services.RegistrationService.RegistrationResult
import io.iohk.atala.prism.models.{DidSuffix, ParticipantId}
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, NodeServiceGrpc}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.node_api
import tofu.Execute

trait RegistrationService[F[_]] {

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      didOrOperation: Either[DID, SignedAtalaOperation]
  ): F[Either[ConnectorError, RegistrationResult]]

}

private class RegistrationServiceImpl[F[_]: MonadThrow](
    participantsRepository: ParticipantsRepository[F],
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ex: Execute[F]
) extends RegistrationService[F] {

  import RegistrationService._

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      didOrOperation: Either[DID, SignedAtalaOperation]
  ): F[Either[ConnectorError, RegistrationResult]] = {
    for {
      maybeCreateRequest <-
        didOrOperation.fold(checkAndUseExistingDID(_, tpe, name, logo), createDID(tpe, name, logo, _))
      _ <- maybeCreateRequest.traverse(participantsRepository.create)
      result = maybeCreateRequest.map { createRequest =>
        RegistrationResult(
          did = createRequest.did,
          id = createRequest.id,
          operationId = createRequest.operationId
        )
      }
    } yield result
  }

  private def createDID(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      createDIDOperation: SignedAtalaOperation
  ): F[Either[ConnectorError, ParticipantsRepository.CreateParticipantRequest]] = {
    for {
      createDIDResponse <-
        ex.deferFuture(nodeService.createDID(node_api.CreateDIDRequest().withSignedOperation(createDIDOperation)))
      did = DID.fromString(DidSuffix.didFromStringSuffix(createDIDResponse.id))
      createRequest =
        ParticipantsRepository
          .CreateParticipantRequest(
            id = ParticipantId.random(),
            tpe = tpe,
            name = name,
            did = did,
            logo = logo,
            operationId = AtalaOperationId
              .fromVectorUnsafe(
                createDIDResponse.operationId.toByteArray.toVector
              )
              .some
          )
          .asRight
    } yield createRequest
  }

  private def checkAndUseExistingDID(
      did: DID,
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo
  ): F[Either[ConnectorError, ParticipantsRepository.CreateParticipantRequest]] = {
    for {
      response <- ex.deferFuture(nodeService.getDidDocument(GetDidDocumentRequest(did.getValue)))
      result =
        response.document
          .toRight(InvalidRequest("the passed DID was not found on the node"))
          .as(
            ParticipantsRepository.CreateParticipantRequest(
              id = ParticipantId.random(),
              tpe = tpe,
              name = name,
              did = did,
              logo = logo,
              operationId = None
            )
          )
    } yield result
  }
}

object RegistrationService {
  def apply[F[_]: BracketThrow: Execute](
      participantsRepository: ParticipantsRepository[F],
      nodeService: NodeServiceGrpc.NodeService
  ): RegistrationService[F] = new RegistrationServiceImpl[F](participantsRepository, nodeService)

  case class RegistrationResult(id: ParticipantId, did: DID, operationId: Option[AtalaOperationId])
}
