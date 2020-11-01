package io.iohk.atala.prism.connector

import java.time.{LocalDateTime, ZoneOffset}

import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.connector.util.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.{connector_api, node_api}
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
        buildInfo.scalaVersion mustBe "2.13.3"
        buildInfo.sbtVersion mustBe "1.3.8"
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
      val did = generateDid(keys.publicKey)
      val request = connector_api.GetCurrentUserRequest()
      (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
    }

    "return the verifier details" in {
      val (publicKey, rpcRequest) = prepareSignedRequest()
      val name = "Verifier"
      val _ = createVerifier(name, Some(publicKey), Some(rpcRequest.did))

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getCurrentUser(rpcRequest.request)
        response.logo.toByteArray mustNot be(empty)
        response.name must be(name)
        response.role must be(connector_api.GetCurrentUserResponse.Role.verifier)
      }
    }

    "return the issuer details" in {
      val (publicKey, rpcRequest) = prepareSignedRequest()

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
      val (_, rpcRequest) = prepareSignedRequest()

      usingApiAs(rpcRequest) { blockingStub =>
        val ex = intercept[StatusRuntimeException] {
          blockingStub.getCurrentUser(rpcRequest.request)
        }
        ex.getStatus.getCode must be(Status.UNKNOWN.getCode)
      }
    }
  }
}
