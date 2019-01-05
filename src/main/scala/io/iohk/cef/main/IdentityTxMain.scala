package io.iohk.cef.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.config.ConfigReaderExtensions._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.frontend.controllers.IdentitiesController
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.{IdentityData, IdentityTransaction}
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object IdentityTxMain extends App {

  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](ConfigFactory.defaultReference())
  val identityTxMainConfig: FrontendConfig =
    pureconfig.loadConfigOrThrow[FrontendConfig](ConfigFactory.load("identity-tx-main"))

  type S = IdentityData
  type T = IdentityTransaction
  type B = Block[S, T]

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("id-tx-system")
  implicit val materializer = ActorMaterializer()

  val nodeTransactionService = CefServices.cefTransactionServiceChannel[S, T](cefConfig)
  val identityTransactionService = new IdentityTransactionService(nodeTransactionService)
  val serviceApi = new IdentitiesController(identityTransactionService)

  val transactionMain = new TransactionMain[S, T](
    serviceApi.routes,
    identityTxMainConfig,
    cefConfig
  )

  transactionMain.run()
}
