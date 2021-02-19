package io.iohk.atala.prism.management.console.services

import io.iohk.atala.prism.DIDGenerator
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.InstitutionGroup
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.{common_models, console_api}

import java.time.Instant

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
      createContact(institutionId, contactName, Some(groupName))
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

    "support time interval" in {
      val institutionName = "tokenizer"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(institutionName, did)

      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val contact1 = createContact(institutionId, "Contact 1")
      createGenericCredential(institutionId, contact1.contactId)

      Thread.sleep(10) // sleep to add some time padding for the inspected interval

      val start = Instant.now()
      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 2"))
      val contact2 = createContact(institutionId, "Contact 2", None)
      createGenericCredential(institutionId, contact2.contactId)
      val end = Instant.now()

      Thread.sleep(10) // sleep to add some time padding for the inspected interval

      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 3"))
      val contact3 = createContact(institutionId, "Contact 3", None)
      createGenericCredential(institutionId, contact3.contactId)

      val request = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(
            start.toEpochMilli,
            end.toEpochMilli
          )
        )
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsConsole(rpcRequest) { serviceStub =>
        val response = serviceStub.getStatistics(request)
        response.numberOfContacts must be(1)
        response.numberOfContactsConnected must be(0)
        response.numberOfContactsPendingConnection must be(0)
        response.numberOfGroups must be(1)
        response.numberOfCredentialsInDraft must be(1)
        response.numberOfCredentialsPublished must be(0)
        response.numberOfCredentialsReceived must be(0)
      }
    }

    "fail when the starting interval timestamp is after the ending interval timestamp" in {
      val institutionName = "tokenizer"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(institutionName, did)

      val start = Instant.now()
      Thread.sleep(10)
      val end = Instant.now()

      val request = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(
            end.toEpochMilli,
            start.toEpochMilli
          )
        )
      )
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAsConsole(rpcRequest) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request))
      }
    }

    "fail when the time interval timestamps are not specified" in {
      val institutionName = "tokenizer"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      createParticipant(institutionName, did)

      val request1 = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval()
        )
      )
      val rpcRequest1 = SignedRpcRequest.generate(keyPair, did, request1)

      usingApiAsConsole(rpcRequest1) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request1))
      }

      val request2 = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(startTimestamp = Instant.now().toEpochMilli)
        )
      )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAsConsole(rpcRequest2) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request2))
      }

      val request3 = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(endTimestamp = Instant.now().toEpochMilli)
        )
      )
      val rpcRequest3 = SignedRpcRequest.generate(keyPair, did, request3)

      usingApiAsConsole(rpcRequest3) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request3))
      }
    }
  }
}
