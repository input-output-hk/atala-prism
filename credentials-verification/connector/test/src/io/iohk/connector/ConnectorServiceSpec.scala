package io.iohk.connector

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.crypto.EC
import io.iohk.connector.model.RequestNonce
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
        nodeMock.getNodeBuildInfo(*).returns {
          Future.successful(node_api.GetNodeBuildInfoResponse().withVersion("node-version"))
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
      val keys = EC.generateKeyPair()
      val privateKey = keys.privateKey
      val request = connector_api.GetCurrentUserRequest()
      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature = EC.sign(
        SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray,
        privateKey
      )
      (requestNonce, signature, keys.publicKey, request)
    }

    "return the verifier details" in {
      val (requestNonce, signature, publicKey, request) = prepareSignedRequest()
      val name = "Verifier"
      val _ = createVerifier(name, Some(publicKey))

      usingApiAs(requestNonce, signature, publicKey) { blockingStub =>
        val response = blockingStub.getCurrentUser(request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.verifier)
      }
    }

    "return the issuer details" in {
      val (requestNonce, signature, publicKey, request) = prepareSignedRequest()

      val name = "Issuer"
      val _ = createIssuer(name, Some(publicKey))

      usingApiAs(requestNonce, signature, publicKey) { blockingStub =>
        val response = blockingStub.getCurrentUser(request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.issuer)
      }
    }

    "fail on unknown user" in {
      val (requestNonce, signature, publicKey, request) = prepareSignedRequest()

      usingApiAs(requestNonce, signature, publicKey) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.getCurrentUser(request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }
}
