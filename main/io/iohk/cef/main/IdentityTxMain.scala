package io.iohk.cef.main

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.codecs.nio.auto._
import io.iohk.cef.config.ConfigReaderExtensions._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.frontend.controllers.{IdentitiesController, SigningKeyPairsController}
import io.iohk.cef.frontend.services.{CryptoService, IdentityTransactionService}
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.{IdentityData, IdentityTransaction}
import io.iohk.cef.ledger.storage.LedgerStorage
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}
import io.iohk.cef.ledger.query.identity.{IdentityQuery, IdentityQueryEngine, IdentityQueryService}
import monix.reactive.MulticastStrategy
import monix.reactive.subjects.ConcurrentSubject
import org.slf4j.LoggerFactory
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object IdentityTxMain extends App {

  val logger = LoggerFactory.getLogger(this.getClass)

  val typesafeConfig = ConfigFactory.defaultReference()
  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](typesafeConfig)
  val identityTxMainConfig: FrontendConfig = pureconfig
    .loadConfigOrThrow[FrontendConfig](ConfigFactory.load())

  type S = IdentityData
  type T = IdentityTransaction
  type B = Block[S, T]
  type Q = IdentityQuery

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("id-tx-system")
  implicit val materializer = ActorMaterializer()

  val ledgerConfig = cefConfig.ledgerConfig

  val ledgerStateStoragePath = Files.createTempFile(s"state-storage-${ledgerConfig.id}", "").toAbsolutePath
  val ledgerStateStorage = new MVLedgerStateStorage[S](ledgerConfig.id, ledgerStateStoragePath)

  val ledgerStoragePath = Files.createTempFile(s"ledger-storage-${ledgerConfig.id}", "").toAbsolutePath
  val ledgerStorage: LedgerStorage[S, T] = new MVLedgerStorage[S, T](ledgerConfig.id, ledgerStoragePath)

  import monix.execution.Scheduler.Implicits.global
  val newBlockChannel = ConcurrentSubject[Block[S, T]](MulticastStrategy.publish)

  newBlockChannel
    .filter(_.transactions.nonEmpty)
    .foreach { newBlock =>
      logger.info(s"New block found, transactions = ${newBlock.transactions.size}")
    }

  val identityQueryEngine = new IdentityQueryEngine(ledgerStateStorage)
  val identityQueryService = new IdentityQueryService(identityQueryEngine)

  val nodeTransactionService =
    CefServices.cefTransactionServiceChannel[S, T, Q](
      cefConfig,
      ledgerStateStorage,
      ledgerStorage,
      identityQueryService,
      newBlockChannel
    )
  val identityTransactionService = new IdentityTransactionService(nodeTransactionService)
  val serviceApi = new IdentitiesController(identityTransactionService)

  val signingKeyPairsController = new SigningKeyPairsController(new CryptoService)

  val transactionMain = CefMain(
    RouteConcatenation.concat(serviceApi.routes, signingKeyPairsController.routes),
    identityTxMainConfig
  )
}
