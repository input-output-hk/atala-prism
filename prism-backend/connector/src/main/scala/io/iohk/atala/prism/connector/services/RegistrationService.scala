package io.iohk.atala.prism.connector.services

import cats.effect.IO
import cats.syntax.either._
import cats.syntax.option._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.connector.errors.{ConnectorError, InvalidRequest}
import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.connector.repositories.ParticipantsRepository
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, NodeServiceGrpc}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.utils.StringUtils.encodeToByteArray

import scala.concurrent.ExecutionContext

class RegistrationService(participantsRepository: ParticipantsRepository[IO], nodeService: NodeServiceGrpc.NodeService)(
    implicit ec: ExecutionContext
) {

  import RegistrationService._

  def register(
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo,
      didOrOperation: Either[PrismDid, SignedAtalaOperation]
  ): FutureEither[ConnectorError, RegistrationResult] = {

    for {
      createRequest <- didOrOperation.fold(checkAndUseExistingDID(_, tpe, name, logo), createDID(tpe, name, logo, _))
      _ <- participantsRepository.create(createRequest).unsafeToFuture().toFutureEither
    } yield RegistrationResult(
      did = createRequest.did,
      id = createRequest.id,
      operationId = createRequest.operationId
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
      did = PrismDid.buildCanonical(SHA256Digest.compute(encodeToByteArray(createDIDResponse.id)))
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
    result.toFutureEither
  }

  private def checkAndUseExistingDID(
      did: PrismDid,
      tpe: ParticipantType,
      name: String,
      logo: ParticipantLogo
  ): FutureEither[ConnectorError, ParticipantsRepository.CreateParticipantRequest] = {
    for {
      _ <-
        nodeService
          .getDidDocument(GetDidDocumentRequest(did.getValue))
          .map(_.document.toRight(InvalidRequest("the passed DID was not found on the node")))
          .toFutureEither
    } yield ParticipantsRepository.CreateParticipantRequest(
      id = ParticipantId.random(),
      tpe = tpe,
      name = name,
      did = did,
      logo = logo,
      operationId = None
    )
  }
}

object RegistrationService {
  case class RegistrationResult(id: ParticipantId, did: PrismDid, operationId: Option[AtalaOperationId])
}
