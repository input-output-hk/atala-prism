package io.iohk.cef.main

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.codecs.nio.auto._
import io.iohk.cef.config.ConfigReaderExtensions._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.frontend.controllers.ChimericTransactionsController
import io.iohk.cef.frontend.services.ChimericTransactionService
import io.iohk.cef.ledger.{Block, LedgerConfig}
import io.iohk.cef.ledger.chimeric.{ChimericStateResult, ChimericTx}
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}
import io.iohk.cef.query.ledger.chimeric.{ChimericQueryEngine, ChimericQueryService}
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ChimericMain extends App {

  val typesafeConfig = ConfigFactory.defaultReference()
  val chimericConfig = ConfigFactory.load("chimeric")
  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](typesafeConfig)
  val frontendConfig: FrontendConfig = pureconfig
    .loadConfigOrThrow[FrontendConfig](chimericConfig)
  val chimericLedgerConfig: LedgerConfig = pureconfig
    .loadConfigOrThrow[LedgerConfig](chimericConfig.getConfig("chimeric-ledger-config"))

  type S = ChimericStateResult
  type T = ChimericTx
  type B = Block[S, T]

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("chimeric-system")
  implicit val materializer = ActorMaterializer()

  val chimericLedgerStateStoragePath =
    Files.createTempFile(s"chimeric-state-storage-${chimericLedgerConfig.id}", "").toAbsolutePath
  val chimericLedgerStateStorage = new MVLedgerStateStorage[S](chimericLedgerConfig.id, chimericLedgerStateStoragePath)
  val chimericLedgerStoragePath =
    Files.createTempFile(s"chimeric-ledger-storage-${chimericLedgerConfig.id}", "").toAbsolutePath
  val chimericLedgerStorage: LedgerStorage[S, T] =
    new MVLedgerStorage[S, T](chimericLedgerConfig.id, chimericLedgerStoragePath)
  val chimericNodeService =
    CefServices.cefTransactionServiceChannel(cefConfig, chimericLedgerStateStorage, chimericLedgerStorage)

  val chimericQueryEngine = new ChimericQueryEngine(chimericLedgerStateStorage)
  val chimericQueryService = new ChimericQueryService(chimericQueryEngine)
  val chimericService = new ChimericTransactionService(chimericNodeService, chimericQueryService)
  val chimericServiceApi = new ChimericTransactionsController(chimericService)

  val routes = chimericServiceApi.routes
  val transactionMain = CefMain(
    routes,
    frontendConfig
  )
}
