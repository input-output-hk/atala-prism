package io.iohk.atala.prism.management.console.integrations

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{
  ParticipantId,
  ParticipantInfo,
  RegisterDID,
  UpdateParticipantProfile
}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class ParticipantsIntegrationService(participantsRepository: ParticipantsRepository[IO]) {

  def register(request: RegisterDID): FutureEither[ManagementConsoleError, Unit] = {
    val createRequest = ParticipantsRepository.CreateParticipantRequest(
      id = ParticipantId.random(),
      name = request.name,
      did = request.did,
      logo = request.logo
    )
    participantsRepository.create(createRequest).unsafeToFuture().toFutureEither
  }

  def getDetails(participantId: ParticipantId): FutureEither[errors.ManagementConsoleError, ParticipantInfo] =
    participantsRepository.findBy(participantId).unsafeToFuture().toFutureEither

  def update(
      participantId: ParticipantId,
      participantProfile: UpdateParticipantProfile
  ): FutureEither[ManagementConsoleError, Unit] = {
    val updateRequest = ParticipantsRepository.UpdateParticipantProfileRequest(
      id = participantId,
      participantProfile
    )
    participantsRepository.update(updateRequest).map(_.asRight).unsafeToFuture().toFutureEither
  }
}
