package io.iohk.atala.prism.connector

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.connector.model.ParticipantLogo
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.protos.{connector_api, node_api}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import scala.concurrent.Future

class ConnectorServiceSpec extends ConnectorRpcSpecBase {

  val logoBytes: Array[Byte] = "none".getBytes()

  "ConnectorService.getBuildInfo" should {
    "return proper build information" in {
      usingApiAs.unlogged { service =>
        nodeMock.getNodeBuildInfo(*).returns {
          Future.successful(
            node_api.GetNodeBuildInfoResponse().withVersion("node-version")
          )
        }

        val buildInfo =
          service.getBuildInfo(connector_api.GetBuildInfoRequest())

        // This changes greatly, so just test something was set
        buildInfo.version must not be empty
        buildInfo.scalaVersion mustBe "2.13.7"
        buildInfo.sbtVersion mustBe "1.5.8"
      }
    }
  }

  "getCurrentUser" should {

    "return the verifier details" in {
      val (publicKey, rpcRequest) =
        prepareSignedRequest(connector_api.GetCurrentUserRequest())
      val name = "Verifier"
      val _ = createVerifier(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getCurrentUser(rpcRequest.request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(
          connector_api.GetCurrentUserResponse.Role.verifier
        )
      }
    }

    "return the verifier details using unpublished did auth" in {
      val (publicKey, rpcRequest) = prepareSignedUnpublishedDidRequest(
        connector_api.GetCurrentUserRequest()
      )
      val name = "Verifier"
      val _ = createVerifier(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getCurrentUser(rpcRequest.request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(
          connector_api.GetCurrentUserResponse.Role.verifier
        )
      }
    }

    "return the issuer details" in {
      val (publicKey, rpcRequest) =
        prepareSignedRequest(connector_api.GetCurrentUserRequest())

      val name = "Issuer"
      val _ = createIssuer(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getCurrentUser(rpcRequest.request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.issuer)
      }
    }

    "fail on unknown user" in {
      val (_, rpcRequest) =
        prepareSignedRequest(connector_api.GetCurrentUserRequest())

      usingApiAs(rpcRequest) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.getCurrentUser(rpcRequest.request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }

  "updateParticipantProfile" should {

    "update the Participant Profile details" in {
      val (publicKey, rpcRequest) = prepareSignedRequest(
        connector_api
          .UpdateProfileRequest(name = "Update Issuer")
          .withLogo(ByteString.copyFrom(logoBytes))
      )

      val name = "Issuer"
      val logo = ParticipantLogo(logoBytes.toVector)
      val participantId =
        createIssuer(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val _ = blockingStub.updateParticipantProfile(rpcRequest.request)
        val result = participantsRepository
          .findBy(participantId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()

        val participantInfo = result.toOption.value
        participantInfo.name must be("Update Issuer")
        participantInfo.logo must be(Some(logo))
      }
    }

    "update the Participant Profile details using unpublished did" in {
      val (publicKey, rpcRequest) = prepareSignedUnpublishedDidRequest(
        connector_api
          .UpdateProfileRequest(name = "Update Issuer")
          .withLogo(ByteString.copyFrom(logoBytes))
      )

      val name = "Issuer"
      val logo = ParticipantLogo(logoBytes.toVector)
      val participantId =
        createIssuer(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val _ = blockingStub.updateParticipantProfile(rpcRequest.request)
        val result = participantsRepository
          .findBy(participantId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()

        val participantInfo = result.toOption.value
        participantInfo.name must be("Update Issuer")
        participantInfo.logo must be(Some(logo))
      }
    }

    "fail on unknown user" in {
      val (_, rpcRequest) = prepareSignedRequest(
        connector_api
          .UpdateProfileRequest(name = "Update Issuer")
          .withLogo(ByteString.copyFrom(logoBytes))
      )

      usingApiAs(rpcRequest) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.updateParticipantProfile(rpcRequest.request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }
}
