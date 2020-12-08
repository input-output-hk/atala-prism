package io.iohk.atala.prism.kycbridge

import monix.eval.{Task, TaskApp}
import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.kycbridge.config.KycBridgeConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.kycbridge.services.{AssureIdService, AssureIdServiceImpl}
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object KycBridgeApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use { assureIdService =>
      logger.info("Kyc bridge application started")

      //only for demonstration purpose
      val device = Device(
        `type` = DeviceType(
          manufacturer = "manufacturer",
          model = "model"
        )
      )

      assureIdService
        .createNewDocumentInstance(device)
        .map(println(_))
        .flatMap(_ => Task.never)
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, AssureIdService] =
    for {
      httpClient <- BlazeClientBuilder[Task](global).resource

      kycBridgeConfig = KycBridgeConfig(ConfigFactory.load(classLoader))

      assureIdService = new AssureIdServiceImpl(kycBridgeConfig.assureIdConfig, httpClient)

    } yield assureIdService
}
