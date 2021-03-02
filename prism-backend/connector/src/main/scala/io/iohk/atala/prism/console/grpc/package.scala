package io.iohk.atala.prism.console

import io.iohk.atala.prism.console.models.{GenericCredential, RevokePublishedCredential}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.protos.console_api.RevokePublishedCredentialRequest

import scala.util.Try

package object grpc {
  implicit val revokePublishedCredentialConverter
      : ProtoConverter[RevokePublishedCredentialRequest, RevokePublishedCredential] = { request =>
    {
      for {
        credentialId <- GenericCredential.Id.from(request.credentialId)
        operation <- Try {
          request.revokeCredentialsOperation
            .getOrElse(throw new RuntimeException("Missing revokeCredentialsOperation"))
        }
        _ <- Try {
          if (operation.operation.exists(_.operation.isRevokeCredentials)) ()
          else throw new RuntimeException("Invalid revokeCredentialsOperation, it is a different operation")
        }

        _ <- Try {
          val credentialHashes = operation.operation
            .flatMap(_.operation.revokeCredentials)
            .map(_.credentialsToRevoke)
            .getOrElse(Seq.empty)

          if (credentialHashes.size == 1) {
            ()
          } else {
            val msg = if (credentialHashes.isEmpty) {
              s"Invalid revokeCredentialsOperation, a single credential is expected but the whole batch was found"
            } else {
              s"Invalid revokeCredentialsOperation, a single credential is expected but ${credentialHashes.size} credentials found"
            }
            throw new RuntimeException(msg)
          }
        }
      } yield RevokePublishedCredential(credentialId, operation)
    }
  }
}
