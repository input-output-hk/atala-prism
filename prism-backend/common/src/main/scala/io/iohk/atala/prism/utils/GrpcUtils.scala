package io.iohk.atala.prism.utils

import java.util.concurrent.TimeUnit
import cats.effect.Resource
import com.typesafe.config.Config
import io.grpc.netty.{GrpcSslContexts, NettyServerBuilder}
import io.grpc.{Server, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService
import monix.eval.Task

import java.io.File
import scala.concurrent.blocking

object GrpcUtils {

  case class GrpcConfig(port: Int)

  object GrpcConfig {
    def apply(config: Config): GrpcConfig = GrpcConfig(config.getInt("grpc.port"))
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

  /**
    * Wrap a [[Server]] into a bracketed resource. The server stops when the
    * resource is released. With the following scenarios:
    *   - Server is shut down when there aren't any requests left.
    *   - We wait for 30 seconds to allow finish pending requests and
    *     then force quit the server.
    */
  def createGrpcServer(
      grpcConfig: GrpcConfig,
      sslConfigOption: Option[SslConfig],
      services: ServerServiceDefinition*
  ): Resource[Task, Server] = {
    val builder = NettyServerBuilder.forPort(grpcConfig.port)

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
      Task {
        server.shutdown()
        if (!blocking(server.awaitTermination(30, TimeUnit.SECONDS))) {
          server.shutdownNow()
        }
      }.void

    Resource.make(Task(builder.build()))(shutdown)
  }
}
