package io.iohk.connector

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import io.grpc.{Status, StatusRuntimeException}
import io.iohk.connector.model.RequestNonce
import io.iohk.cvp.crypto.ECKeys.toEncodePublicKey
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.SignedRequestsHelper
import io.iohk.prism.protos.{connector_api, node_api}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._

import scala.concurrent.Future

class ConnectorServiceSpec extends ConnectorRpcSpecBase {
  "ConnectorService.getBuildInfo" should {
    "return proper build information" in {
      usingApiAs.unlogged { service =>
        // Use a month so that's long enough to not cache the build date but short enough to be helpful for the test
        val aMonthAgo = LocalDateTime.now(ZoneOffset.UTC).minusMonths(1)
        nodeMock.getBuildInfo(*).returns {
          Future.successful(node_api.GetBuildInfoResponse().withVersion("node-version"))
        }

        val buildInfo = service.getBuildInfo(connector_api.GetBuildInfoRequest())

        // This changes greatly, so just test something was set
        buildInfo.version must not be empty
        buildInfo.scalaVersion mustBe "2.12.10"
        buildInfo.millVersion mustBe "0.6.2"
        // Give it enough time between build creation and test
        val buildTime = LocalDateTime.parse(buildInfo.buildTime)
        buildTime.compareTo(aMonthAgo) must be > 0
        buildInfo.nodeVersion mustBe "node-version"
      }
    }
  }

  "getCurrentUser" should {
    def prepareSignedRequest() = {
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val encodedPublicKey = toEncodePublicKey(keys.getPublic)
      val request = connector_api.GetCurrentUserRequest()
      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature = ECSignature.sign(
        privateKey,
        SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
      )
      (requestNonce, signature, encodedPublicKey, request)
    }

    "return the verifier details" in {
      val (requestNonce, signature, encodedPublicKey, request) = prepareSignedRequest()
      val name = "Verifier"
      val _ = createVerifier(name, Some(encodedPublicKey))

      usingApiAs(requestNonce, signature, encodedPublicKey) { blockingStub =>
        val response = blockingStub.getCurrentUser(request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.verifier)
      }
    }

    "return the issuer details" in {
      val (requestNonce, signature, encodedPublicKey, request) = prepareSignedRequest()

      val name = "Issuer"
      val _ = createIssuer(name, Some(encodedPublicKey))

      usingApiAs(requestNonce, signature, encodedPublicKey) { blockingStub =>
        val response = blockingStub.getCurrentUser(request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.issuer)
      }
    }

    "fail on unknown user" in {
      val (requestNonce, signature, encodedPublicKey, request) = prepareSignedRequest()

      usingApiAs(requestNonce, signature, encodedPublicKey) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.getCurrentUser(request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }
}
