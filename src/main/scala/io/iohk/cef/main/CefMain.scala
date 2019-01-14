package io.iohk.cef.main

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.config.CefServices
import io.iohk.cef.utils.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CefMain private (bindingFuture: Future[Http.ServerBinding])(
    implicit executionContext: ExecutionContext,
    system: ActorSystem)
    extends Logger {

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
    log.info("Shutting down CEF")
    bindingFuture.flatMap(_.terminate(hardDeadline = 1.minute)).map { _ =>
      CefServices.shutdown
      log.info("Shut down complete")
      Done
    }
  }
}

object CefMain {
  def apply(route: Route, frontendConfig: FrontendConfig)(
      implicit actorSystem: ActorSystem,
      executionContext: ExecutionContext,
      timeout: Timeout,
      materializer: ActorMaterializer): CefMain = {

    val binding = Http()(actorSystem)
      .bindAndHandle(route, frontendConfig.bindAddress.getHostName, frontendConfig.bindAddress.getPort)
    new CefMain(binding)
  }
}
