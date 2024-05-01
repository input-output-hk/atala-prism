package io.iohk.atala.prism.node

import cats.effect.IO
import io.grpc._
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import org.scalatest.BeforeAndAfterEach
import tofu.logging.Logs

import _root_.java.util.concurrent.TimeUnit

abstract class RpcSpecBase extends AtalaWithPostgresSpec with BeforeAndAfterEach {

  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _

  val testLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  def services: Seq[ServerServiceDefinition]

  override def beforeEach(): Unit = {
    super.beforeEach()

    serverName = InProcessServerBuilder.generateName()

    val serverBuilderWithoutServices = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()

    val serverBuilder = services.foldLeft(serverBuilderWithoutServices) { (builder, service) =>
      builder.addService(service)
    }

    serverHandle = serverBuilder.build().start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()
  }

  override def afterEach(): Unit = {
    // Gracefully shut down the channel with a 10s deadline and then force it to ensure it's released
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    channelHandle.shutdownNow()
    // Gracefully shut down the server with a 10s deadline and then force it to ensure it's released
    serverHandle.shutdown()
    serverHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdownNow()

    super.afterEach()
  }
}
