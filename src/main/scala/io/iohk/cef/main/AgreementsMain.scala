package io.iohk.cef.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.config.ConfigReaderExtensions._
import io.iohk.cef.config.{CefConfig, CefServices}
import io.iohk.cef.data.DataItem
import io.iohk.cef.frontend.controllers.AgreementsGenericController
import play.api.libs.json.{Format, Json}
import pureconfig.generic.auto._
import io.iohk.cef.frontend.controllers.common.Codecs._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AgreementsMain extends App {

  val cefConfig: CefConfig = pureconfig.loadConfigOrThrow[CefConfig](ConfigFactory.defaultReference())
  val agreementsMainConfig: FrontendConfig =
    pureconfig.loadConfigOrThrow[FrontendConfig](ConfigFactory.load("agreements-main"))

  case class Certificate(id: String, date: String)
  implicit val format: Format[Certificate] = Json.format[Certificate]

  implicit val timeout: Timeout = 1 minute
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val actorSystem = ActorSystem("agreements-system")
  implicit val materializer = ActorMaterializer()

  val agreementsService = CefServices.cefAgreementsServiceChannel[DataItem[Certificate]](cefConfig)
  val serviceApi = new AgreementsGenericController()
  val prefix = "certificates"
  val agreementsMain = CefMain(
    serviceApi.routes(prefix,agreementsService),
    agreementsMainConfig
  )
}
