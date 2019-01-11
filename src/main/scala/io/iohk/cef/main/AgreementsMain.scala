package io.iohk.cef.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.cef.agreements.AgreementsMessage.Agree
import io.iohk.cef.network.NodeId
import io.iohk.cef.agreements.AgreementsMessage.Propose
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.config.ConfigReaderExtensions._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.frontend.controllers.AgreementsGenericController
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// Usage -Dconfig.resource=agreements-node-[1|2]-app.conf
object AgreementsMain extends App {
  args.foreach(println)
  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](ConfigFactory.load(args(0)))
  val agreementsMainConfig: FrontendConfig = pureconfig.loadConfigOrThrow[FrontendConfig](ConfigFactory.load(args(0)))

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("agreements-system")
  implicit val materializer = ActorMaterializer()

  val agreementsService = CefServices.cefAgreementsServiceChannel[String](cefConfig)
  // our simple app will print proposals received.
  agreementsService.agreementEvents.foreach({
    case p:Propose[String] =>
      println(s"Node '${cefConfig.peerConfig.nodeId}' has received proposal '$p'")
      //For testing without curl
      if(p.data == "hello there") agreementsService.agree(p.correlationId, p.data)
    case a:Agree[String] =>
      println(s"Node '${cefConfig.peerConfig.nodeId}' has received agreements '$a'")
    case _ => ()
  })

  val serviceApi = new AgreementsGenericController()
  val prefix = "weather"
  val agreementsMain = CefMain(
    serviceApi.routes(prefix,agreementsService),
    agreementsMainConfig
  )
  Thread.sleep(2000)
  agreementsService.propose("an-id", "hello there", List(NodeId("2222")))
}
