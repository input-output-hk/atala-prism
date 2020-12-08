package io.iohk.atala.prism.kycbridge

import monix.eval.{Task, TaskApp}
import cats.effect.{ExitCode, Resource}
import com.typesafe.config.ConfigFactory
import io.iohk.atala.prism.kycbridge.config.KycBridgeConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DeviceType}
import io.iohk.atala.prism.kycbridge.services.{AcasServiceImpl, AssureIdServiceImpl}
import org.http4s.client.blaze.BlazeClientBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

object KycBridgeApp extends TaskApp {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def run(args: List[String]): Task[ExitCode] = {
    val classLoader = Thread.currentThread().getContextClassLoader
    app(classLoader).use {
      case (assureIdService, acasService) =>
        logger.info("Kyc bridge application started")

        //only for demonstration purpose
        val device = Device(
          `type` = DeviceType(
            manufacturer = "manufacturer",
            model = "model"
          )
        )

        (for {
          documentResponse <- assureIdService.createNewDocumentInstance(device)
          _ = logger.info(s"New document response: $documentResponse")
          accessTokenResponse <- acasService.getAccessToken
          _ = logger.info(s"Access token response: $accessTokenResponse")
          documentStatus <- assureIdService.getDocumentStatus(
            documentResponse.toOption.get.documentId,
            accessTokenResponse.toOption.get.accessToken
          )
          _ = logger.info(s"Document status: $documentStatus")
        } yield ()).flatMap(_ => Task.never)
    }
  }

  def app(classLoader: ClassLoader): Resource[Task, (AssureIdServiceImpl, AcasServiceImpl)] =
    for {
      httpClient <- BlazeClientBuilder[Task](global).resource

      kycBridgeConfig = KycBridgeConfig(ConfigFactory.load(classLoader))

      assureIdService = new AssureIdServiceImpl(kycBridgeConfig.acuantConfig, httpClient)
      acasService = new AcasServiceImpl(kycBridgeConfig.acuantConfig, httpClient)

    } yield (assureIdService, acasService)
}
