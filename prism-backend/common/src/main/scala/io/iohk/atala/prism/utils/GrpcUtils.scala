package io.iohk.atala.prism.utils

import java.util.concurrent.TimeUnit

import cats.effect.Resource
import com.typesafe.config.Config
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}
import io.grpc.protobuf.services.ProtoReflectionService
import monix.eval.Task

import scala.concurrent.blocking

object GrpcUtils {

  case class GrpcConfig(port: Int)

  object GrpcConfig {
    def apply(config: Config): GrpcConfig = GrpcConfig(config.getInt("grpc.port"))
  }

  /**
    * Wrap a [[Server]] into a bracketed resource. The server stops when the
    * resource is released. With the following scenarios:
    *   - Server is shut down when there aren't any requests left.
    *   - We wait for 30 seconds to allow finish pending requests and
    *     then force quit the server.
    */
  def createGrpcServer(grpcConfig: GrpcConfig, services: ServerServiceDefinition*): Resource[Task, Server] = {
    val builder = ServerBuilder.forPort(grpcConfig.port)

    builder.addService(
      ProtoReflectionService.newInstance()
    ) // TODO: Decide before release if we should keep this (or guard it with a config flag)
    services.foreach(builder.addService(_))

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
