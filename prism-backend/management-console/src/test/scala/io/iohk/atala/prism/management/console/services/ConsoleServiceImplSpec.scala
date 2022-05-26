package io.iohk.atala.prism.management.console.services

import cats.effect.unsafe.implicits.global
import cats.syntax.option._
import com.google.protobuf.ByteString
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{InstitutionGroup, ParticipantLogo}
import io.iohk.atala.prism.management.console.{DataPreparation, ManagementConsoleRpcSpecBase}
import io.iohk.atala.prism.models.AtalaOperationId
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_models.{CreateDIDOutput, OperationOutput}
import io.iohk.atala.prism.protos.{common_models, console_api, node_api}
import io.iohk.atala.prism.utils.syntax._
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

class ConsoleServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil {

  "health check" should {
    "respond" in {
      consoleService.healthCheck(HealthCheckRequest()).futureValue must be(
        HealthCheckResponse()
      )
    }
  }

  "getStatistics" should {
    "work" in {
      val institutionName = "tokenizer"
      val groupName = InstitutionGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.getPublicKey
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
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      val institutionId = createParticipant(institutionName, did)

      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 1"))
      val contact1 = createContact(institutionId, "Contact 1")
      createGenericCredential(institutionId, contact1.contactId, "A")

      Thread.sleep(
        10
      ) // sleep to add some time padding for the inspected interval

      val start = Instant.now()
      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 2"))
      val contact2 = createContact(institutionId, "Contact 2", None)
      createGenericCredential(institutionId, contact2.contactId, "B")
      val end = Instant.now()

      Thread.sleep(
        10
      ) // sleep to add some time padding for the inspected interval

      createInstitutionGroup(institutionId, InstitutionGroup.Name("Grp 3"))
      val contact3 = createContact(institutionId, "Contact 3", None)
      createGenericCredential(institutionId, contact3.contactId, "C")

      val request = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(
            start.toProtoTimestamp.some,
            end.toProtoTimestamp.some
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
      val publicKey = keyPair.getPublicKey
      val did = generateDid(publicKey)
      createParticipant(institutionName, did)

      val start = Instant.now()
      Thread.sleep(10)
      val end = Instant.now()

      val request = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(
            end.toProtoTimestamp.some,
            start.toProtoTimestamp.some
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
      val publicKey = keyPair.getPublicKey
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
          common_models.TimeInterval(startTimestamp = Instant.now().toProtoTimestamp.some)
        )
      )
      val rpcRequest2 = SignedRpcRequest.generate(keyPair, did, request2)

      usingApiAsConsole(rpcRequest2) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request2))
      }

      val request3 = console_api.GetStatisticsRequest(
        Some(
          common_models.TimeInterval(startTimestamp = Instant.now().toProtoTimestamp.some)
        )
      )
      val rpcRequest3 = SignedRpcRequest.generate(keyPair, did, request3)

      usingApiAsConsole(rpcRequest3) { serviceStub =>
        intercept[RuntimeException](serviceStub.getStatistics(request3))
      }
    }
  }

  "registerDID" should {
    def doTest(
        did: DID,
        operationId: AtalaOperationId,
        request: console_api.RegisterConsoleDIDRequest
    ) = {
      val createDidResponse = OperationOutput(
        OperationOutput.Result.CreateDidOutput(CreateDIDOutput(did.getSuffix)),
        OperationOutput.OperationMaybe.OperationId(operationId.toProtoByteString)
      )
      usingApiAsConsole.unlogged { serviceStub =>
        nodeMock.scheduleOperations(*).returns {
          Future.successful(
            node_api.ScheduleOperationsResponse(List(createDidResponse))
          )
        }
        serviceStub.registerDID(request)
      }
    }

    "work with logo" in {
      val did = DataPreparation.newDID()
      val operationId = AtalaOperationId.random()
      val request = console_api
        .RegisterConsoleDIDRequest()
        .withDid(did.getValue)
        .withName("iohk")
        .withLogo(ByteString.copyFrom("logo".getBytes()))

      doTest(did, operationId, request)
    }

    "work without logo" in {
      val did = DataPreparation.newDID()
      val operationId = AtalaOperationId.random()
      val request = console_api
        .RegisterConsoleDIDRequest()
        .withDid(did.getValue)
        .withName("iohk")

      doTest(did, operationId, request)
    }

    "fail when the did is missing" in {
      val did = DataPreparation.newDID()
      val operationId = AtalaOperationId.random()
      val request = console_api
        .RegisterConsoleDIDRequest()
        .withName("iohk")

      intercept[StatusRuntimeException] {
        doTest(did, operationId, request)
      }
    }

    "fail when the did is invalid" in {
      val did = DataPreparation.newDID()
      val operationId = AtalaOperationId.random()
      val request = console_api
        .RegisterConsoleDIDRequest()
        .withDid("this is not a did!")
        .withName("iohk")

      intercept[StatusRuntimeException] {
        doTest(did, operationId, request)
      }
    }

    "fail when user tries register the same did twice" in {
      val did = DataPreparation.newDID()
      val operationId = AtalaOperationId.random()
      val request = console_api
        .RegisterConsoleDIDRequest()
        .withDid(did.getValue)
        .withName("iohk")

      doTest(did, operationId, request)
      val mustBeError = Try(doTest(did, operationId, request))
      mustBeError.toEither.left.map(_.getMessage) mustBe Left(
        "INVALID_ARGUMENT: DID already exists"
      )
    }
  }

  "getCurrentUser" should {
    def prepareSignedRequest() = {
      val keys = EC.generateKeyPair()
      val did = generateDid(keys.getPublicKey)
      val request = console_api.GetConsoleCurrentUserRequest()
      SignedRpcRequest.generate(keys, did, request)
    }

    "return the details" in {
      val rpcRequest = prepareSignedRequest()
      val name = "iohk"
      createParticipant(name, rpcRequest.did)

      usingApiAsConsole(rpcRequest) { blockingStub =>
        val response = blockingStub.getCurrentUser(rpcRequest.request)
        response.logo.toByteArray must be(empty)
        response.name must be(name)
      }
    }

    "fail on unknown user" in {
      val rpcRequest = prepareSignedRequest()

      usingApiAsConsole(rpcRequest) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.getCurrentUser(rpcRequest.request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }

  "updateParticipantProfile" should {
    def prepareSignedRequest() = {
      val keys = EC.generateKeyPair()
      val did = generateDid(keys.getPublicKey)
      val request =
        console_api
          .ConsoleUpdateProfileRequest()
          .withName("iohk updated")
          .withLogo(ByteString.copyFrom("logo".getBytes()))
      SignedRpcRequest.generate(keys, did, request)
    }

    "update the Participant Profile details" in {
      val rpcRequest = prepareSignedRequest()
      val name = "iohk"
      val logo = ParticipantLogo("logo".getBytes().toVector)

      val participantId = createParticipant(name, rpcRequest.did)

      usingApiAsConsole(rpcRequest) { blockingStub =>
        blockingStub.updateParticipantProfile(rpcRequest.request)
        val result = participantsRepository
          .findBy(participantId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
        val participantInfo = result.toOption.value
        participantInfo.name must be("iohk updated")
        participantInfo.logo must be(Some(logo))
      }
    }

    "fail on unknown ParticipantId" in {
      val rpcRequest = prepareSignedRequest()

      usingApiAsConsole(rpcRequest) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.updateParticipantProfile(rpcRequest.request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }
}
