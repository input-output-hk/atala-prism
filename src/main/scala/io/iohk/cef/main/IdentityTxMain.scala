package io.iohk.cef.main

import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.crypto._
import io.iohk.cef.frontend.controllers.IdentitiesController
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.frontend.services.IdentityTransactionService
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.IdentityTransaction
import pureconfig.generic.auto._
import io.iohk.cef.config.ConfigReaderExtensions._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.io.StdIn

object IdentityTxMain extends App {

  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](ConfigFactory.defaultReference())
  val identityTxMainConfig: IdentityTxMainConfig =
    pureconfig.loadConfigOrThrow[IdentityTxMainConfig](ConfigFactory.load("identity-tx-main"))

  type S = Set[SigningPublicKey]
  type T = IdentityTransaction
  type B = Block[S, T]

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("id-tx-system")
  implicit val materializer = ActorMaterializer()

  val nodeCore = CefServices.cefCoreChannel[S, T](cefConfig)
  val identityTransactionService = new IdentityTransactionService(nodeCore)

  val serviceApi = new IdentitiesController(identityTransactionService)
  val bindAddress = cefConfig.peerConfig.networkConfig.tcpTransportConfig.get.bindAddress
  val route = serviceApi.routes
  val serverBinding = Http()(actorSystem)
    .bindAndHandle(route, identityTxMainConfig.bindAddress.getHostName, identityTxMainConfig.bindAddress.getPort)

  StdIn.readLine() // let it run until user presses return
  Await.result(serverBinding.flatMap(_.unbind()), 1 minute) // trigger unbinding from the port

  val pair = generateSigningKeyPair()
  val ed = Base64.getEncoder
  val encodedKey = ed.encodeToString(pair.public.toByteString.toArray)
  val signature = IdentityTransaction.sign("carlos", IdentityTransactionType.Claim, pair.public, pair.`private`)
  println(encodedKey)
  println(ed.encodeToString(signature.toByteString.toArray))
}
