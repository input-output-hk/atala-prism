package io.iohk.atala.prism.management.console.integrations

import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.errors.ManagementConsoleError
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo, RegisterDID}
import io.iohk.atala.prism.management.console.repositories.ParticipantsRepository
import io.iohk.atala.prism.utils.FutureEither

class ParticipantsIntegrationService(participantsRepository: ParticipantsRepository) {

  def register(request: RegisterDID): FutureEither[ManagementConsoleError, Unit] = {
    val createRequest = ParticipantsRepository.CreateParticipantRequest(
      id = ParticipantId.random(),
      name = request.name,
      did = request.did,
      logo = request.logo
    )
    participantsRepository.create(createRequest)
  }

  def getDetails(participantId: ParticipantId): FutureEither[errors.ManagementConsoleError, ParticipantInfo] = {
    participantsRepository.findBy(participantId)
  }
}
