package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.InstitutionGroup
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.console_api

class ConsoleServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDGenerator {

  "health check" should {
    "respond" in {
      consoleService.healthCheck(HealthCheckRequest()).futureValue must be(HealthCheckResponse())
    }
  }

  "getStatistics" should {
    "work" in {
      val institutionName = "tokenizer"
      val groupName = InstitutionGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(institutionName, did)
      createInstitutionGroup(institutionId, groupName)
      createContact(institutionId, contactName, groupName)
      val request = console_api.GetStatisticsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsConsole(rpcRequest) { serviceStub =>
        val response = serviceStub.getStatistics(request)
        response.numberOfContacts must be(1)
        response.numberOfContactsConnected must be(0)
        response.numberOfContactsPendingConnection must be(0)
        response.numberOfGroups must be(1)
        response.numberOfCredentialsInDraft must be(0)
        response.numberOfCredentialsPublished must be(0)
        response.numberOfCredentialsReceived must be(0)
      }
    }
  }
}
