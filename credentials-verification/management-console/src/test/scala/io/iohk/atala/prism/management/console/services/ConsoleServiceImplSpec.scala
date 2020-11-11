package io.iohk.atala.prism.management.console.services

import java.util.UUID

import cats.effect.IO
import doobie.util.transactor.Transactor
import doobie.implicits._
import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}
import io.iohk.atala.prism.management.console.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api

class ConsoleServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {
  def createIssuer(
      name: String,
      did: String
  )(implicit
      database: Transactor[IO]
  ): ParticipantId = {
    val id = ParticipantId(UUID.randomUUID())
    val participant = ParticipantInfo(id, name, did, None)
    ParticipantsDAO.insert(participant).transact(database).unsafeRunSync()

    id
  }

  "health check" should {
    "respond" in {
      consoleService.healthCheck(HealthCheckRequest()).futureValue must be(HealthCheckResponse())
    }
  }

  "authentication" should {
    "support unpublished DID authentication" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createIssuer("Issuer", did)
      val request = console_api.CreateContactRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.createContact(request)
        response must be(console_api.CreateContactResponse())
      }
    }
  }
}
