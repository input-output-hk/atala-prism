package io.iohk.atala.prism.node.utils

import java.util.concurrent.TimeUnit
import cats.effect.{Resource, Sync}
import com.typesafe.config.Config
import io.grpc.netty.{GrpcSslContexts, NettyServerBuilder}
import io.grpc.{Channel, ManagedChannelBuilder, Server, ServerInterceptor, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService

import java.io.File
import scala.concurrent.blocking
import io.grpc.stub.AbstractStub
import io.iohk.atala.prism.protos.node_api.ScheduleOperationsResponse
import io.iohk.atala.prism.protos.node_models.OperationOutput

object GrpcUtils {

  def extractSingleOperationOutput(scheduleOperationsResponse: ScheduleOperationsResponse): OperationOutput =
    scheduleOperationsResponse.outputs match {
      case Seq(head) => head
      case outputs =>
        throw new RuntimeException(s"1 operation output expected but got ${outputs.size}")
    }

  case class GrpcConfig(port: Int)

  object GrpcConfig {
    def apply(config: Config): GrpcConfig = GrpcConfig(
      config.getInt("grpc.port")
    )
  }

  case class SslConfig(
      serverCertificateLocation: String,
      serverCertificatePrivateKeyLocation: String,
      serverTrustChainLocation: String
  )

  object SslConfig {
    def apply(config: Config): SslConfig =
      SslConfig(
        serverCertificateLocation = config.getString("ssl.serverCertificateLocation"),
        serverCertificatePrivateKeyLocation = config.getString("ssl.serverCertificatePrivateKeyLocation"),
        serverTrustChainLocation = config.getString("ssl.serverTrustChainLocation")
      )
  }

  /** Create a plainttext stub with a given host and port.
    */
  def createPlaintextStub[S <: AbstractStub[S]](
      host: String,
      port: Int,
      stub: Channel => S
  ): S = {
    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .build()

    stub(channel)
  }

  /** Wrap a Server into a bracketed resource. The server stops when the resource is released. With the following
    * scenarios:
    *   - Server is shut down when there aren't any requests left.
    *   - We wait for 30 seconds to allow finish pending requests and then force quit the server.
    */
  def createGrpcServer[F[_]: Sync](
      grpcConfig: GrpcConfig,
      sslConfigOption: Option[SslConfig],
      interceptor: List[ServerInterceptor],
      services: ServerServiceDefinition*
  ): Resource[F, Server] = {
    val builder = NettyServerBuilder
      .forPort(grpcConfig.port)
    interceptor.foreach(builder.intercept(_))

    builder.addService(
      ProtoReflectionService.newInstance()
    ) // TODO: Decide before release if we should keep this (or guard it with a config flag)
    services.foreach(builder.addService(_))

    sslConfigOption.foreach { sslConfig =>
      val sslContext = GrpcSslContexts.forServer(
        new File(sslConfig.serverCertificateLocation),
        new File(sslConfig.serverCertificatePrivateKeyLocation)
      )

      builder.sslContext(sslContext.build())
    }

    val shutdown = (server: Server) =>
      Sync[F].delay {
        server.shutdown()
        if (!blocking(server.awaitTermination(30, TimeUnit.SECONDS))) {
          server.shutdownNow()
        }
        ()
      }

    Resource.make(Sync[F].delay(builder.build()))(shutdown)
  }
}
