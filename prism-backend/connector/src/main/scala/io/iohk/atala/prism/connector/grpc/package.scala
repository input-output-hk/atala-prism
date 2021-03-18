package io.iohk.atala.prism.connector

import io.iohk.atala.prism.connector.model.{ParticipantLogo, UpdateParticipantProfile}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.connector_api.UpdateProfileRequest
import scala.util.Try

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
}
