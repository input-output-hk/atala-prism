package io.iohk.atala.prism.connector

import cats.data.NonEmptyList
import io.iohk.atala.prism.connector.model.{ParticipantLogo, UpdateParticipantProfile, SendMessages}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.connector_api.UpdateProfileRequest
import scala.util.Try
import io.iohk.atala.prism.protos.connector_api

package object grpc {
  implicit val participantProfileConverter: ProtoConverter[UpdateProfileRequest, UpdateParticipantProfile] = {
    request =>
      {
        for {
          name <- Try {
            if (request.name.trim.isEmpty) throw new RuntimeException("The name is required")
            else request.name.trim
          }

          logo <- Try {
            val bytes = request.logo.toByteArray
            if (bytes.isEmpty) None
            else Some(ParticipantLogo(bytes.toVector))
          }
        } yield UpdateParticipantProfile(name, logo)
      }
  }

  implicit val sendMessagesConverter: ProtoConverter[connector_api.SendMessagesRequest, SendMessages] = {
    (request: connector_api.SendMessagesRequest) =>
      Try(SendMessages(NonEmptyList.fromList(request.messagesByConnectionToken.toList)))
  }

}
