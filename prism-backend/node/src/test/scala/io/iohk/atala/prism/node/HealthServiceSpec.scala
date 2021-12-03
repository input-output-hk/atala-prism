package io.iohk.atala.prism.node

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import _root_.grpc.health.v1.health
import org.scalatest.concurrent.ScalaFutures
import io.grpc.stub.StreamObserver
import scala.concurrent.ExecutionContext
import io.grpc.inprocess.InProcessServerBuilder
import org.scalatest.BeforeAndAfterEach
import io.grpc.Server
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import java.util.concurrent.TimeUnit

/** HealthServiceSpec
  *
  * > testOnly **.HealthServiceSpec
  */
class HealthServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  private val healthService = new HealthService

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var service: health.HealthGrpc.HealthBlockingStub = _

  override def beforeEach(): Unit = {
    super.beforeEach()
    serverName = InProcessServerBuilder.generateName()
    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(_root_.grpc.health.v1.health.HealthGrpc.bindService(healthService, executionContext))
      .build()
      .start()
    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()
    service = health.HealthGrpc.blockingStub(channelHandle)
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  "HealthService" should {
    "reply to a check request" in {
      val ret = healthService.check(health.HealthCheckRequest(service = "Tester")).futureValue
      // Example of the log
      // {"@timestamp":"2021-12-03T17:46:36.155Z","loggerName":"io.iohk.atala.prism.node.HealthService",
      //  "threadName":"io-compute-5","level":"INFO","message":"Replying to health request, service = Tester",
      //  "traceId":"91879d92-2f05-44e1-95a9-b3a2b5ccd279"}
      ret.status mustBe health.HealthCheckResponse.ServingStatus.SERVING
    }

    "reply to all events to watch stream" in {
      val streamObserver = new StreamObserver[health.HealthCheckResponse]() {
        def onCompleted(): Unit = ()
        def onError(x$1: Throwable): Unit = { fail("Shound not fail") }
        def onNext(ret: health.HealthCheckResponse): Unit = {
          ret.status mustBe health.HealthCheckResponse.ServingStatus.SERVING
          ()
        }
      }

      healthService.watch(health.HealthCheckRequest(service = "Tester1"), streamObserver)
      healthService.watch(health.HealthCheckRequest(service = "Tester2"), streamObserver)
      healthService.watch(health.HealthCheckRequest(service = "Tester3"), streamObserver)
    }

    "HealthService Stub" should {
      val request = health.HealthCheckRequest(service = "TesterStub")
      "reply to a check" in {
        val ret = service.check(request)
        // Example of the log
        // {"@timestamp":"2021-12-03T17:46:36.232Z","loggerName":"io.iohk.atala.prism.node.HealthService","threadName":"io-compute-22",
        //  "level":"INFO","message":"Replying to health request, service = TesterStub","traceId":"d28895e5-6416-44b7-aeaf-749ea898fa26"}
        ret.status mustBe health.HealthCheckResponse.ServingStatus.SERVING
      }
    }
  }

}
