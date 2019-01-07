package io.iohk.cef.main

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.ledger.Transaction
import sun.misc.Signal

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

class TransactionMain[S, T <: Transaction[S]] private (route: Route,
                                                       frontendConfig: FrontendConfig,
                                                       cefConfig: CefConfig,
                                                       bindingFuture: Future[Http.ServerBinding])
                                             (implicit executionContext: ExecutionContext) {

  Signal.handle(new Signal("TERM"), onSigTerm)

  def run(): Future[Unit] = {
    println("Press enter to shutdown")
    StdIn.readLine() // let it run until user presses return
    shutdown()
  }

  private def onSigTerm(signal: Signal): Unit = {
    shutdown()
  }

  private def shutdown(): Future[Unit] = {
    for {
      binding <- bindingFuture
      _ <- binding.unbind()
    } yield {
      CefServices.shutdown
    }
  }
}

object TransactionMain {
  def apply[S, T <: Transaction[S]](route: Route,
            frontendConfig: FrontendConfig,
            cefConfig: CefConfig)
           (implicit actorSystem: ActorSystem, executionContext: ExecutionContext, timeout: Timeout, materializer: ActorMaterializer) = {

    val binding = Http()(actorSystem)
      .bindAndHandle(route, frontendConfig.bindAddress.getHostName, frontendConfig.bindAddress.getPort)
    new TransactionMain[S, T](route, frontendConfig, cefConfig, binding)
  }
}
