package io.iohk.connector

import java.time.{LocalDateTime, ZoneOffset}

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
}
