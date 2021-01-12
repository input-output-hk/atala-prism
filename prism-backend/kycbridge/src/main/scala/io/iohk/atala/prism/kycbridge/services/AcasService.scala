package io.iohk.atala.prism.kycbridge.services

import io.iohk.atala.prism.kycbridge.config.AcuantConfig
import io.iohk.atala.prism.kycbridge.models.acas.{AccessTokenRequestBody, AccessTokenResponseBody}
import io.iohk.atala.prism.kycbridge.services.ServiceUtils.{basicAuthorization, runRequestToEither}
import monix.eval.Task
import org.http4s.Method.POST
import org.http4s.{MediaType, Uri}
import org.http4s.client.Client
import org.http4s.headers.Accept
import org.http4s.circe._
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.models.acas.implicits._
import org.http4s.client.dsl.Http4sClientDsl

trait AcasService {
  def getAccessToken: Task[Either[Exception, AccessTokenResponseBody]]
}

class AcasServiceImpl(acuantConfig: AcuantConfig, client: Client[Task]) extends AcasService with Http4sClientDsl[Task] {

  private val baseUri = Uri.unsafeFromString(acuantConfig.acasUrl)

  private lazy val authorization = basicAuthorization(acuantConfig)

  def getAccessToken: Task[Either[Exception, AccessTokenResponseBody]] = {

    val request = POST(
      AccessTokenRequestBody().asJson,
      baseUri / "oauth/token",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[AccessTokenResponseBody](request, client)
  }
}
