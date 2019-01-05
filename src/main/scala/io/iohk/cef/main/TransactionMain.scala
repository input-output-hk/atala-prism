package io.iohk.cef.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.ledger.Transaction

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

class TransactionMain[S, T <: Transaction[S]](route: Route,
                                              frontendConfig: FrontendConfig,
                                              cefConfig: CefConfig)
                                             (implicit actorSystem: ActorSystem, executionContext: ExecutionContext, timeout: Timeout, materializer: ActorMaterializer) {

  private def startup(): Future[Http.ServerBinding] = {
    Http()(actorSystem)
      .bindAndHandle(route, frontendConfig.bindAddress.getHostName, frontendConfig.bindAddress.getPort)
  }
  
  private def shutdown(binding: Http.ServerBinding): Future[Unit] = {
    binding.unbind().map(_ => CefServices.shutdown)
  }

  def run(): Future[Unit] = {
    val binding = startup()
    println("Press enter to shutdown")
    StdIn.readLine() // let it run until user presses return
    binding.map(shutdown)
  }
}
