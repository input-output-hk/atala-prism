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
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.models.acas.implicits._
import io.iohk.atala.prism.kycbridge.services.AcasService.AcasServiceError
import org.http4s.client.dsl.Http4sClientDsl
import cats.implicits._

trait AcasService {
  def getAccessToken: Task[Either[AcasServiceError, AccessTokenResponseBody]]
}

object AcasService {
  case class AcasServiceError(methodName: String, throwable: Throwable) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Error occurred when calling Acuant Acas service method: $methodName, cause: ${throwable.getMessage}"
      )
    }
  }
}

class AcasServiceImpl(acuantConfig: AcuantConfig, client: Client[Task]) extends AcasService with Http4sClientDsl[Task] {

  private implicit class AcasServiceErrorOps[A](value: Task[Either[Exception, A]]) {
    def mapExceptionToServiceError(methodName: String): Task[Either[AcasServiceError, A]] =
      value.map(_.leftMap(e => AcasServiceError(methodName, e)))
  }

  private val baseUri = Uri.unsafeFromString(acuantConfig.acasUrl)

  private lazy val authorization = basicAuthorization(acuantConfig)

  def getAccessToken: Task[Either[AcasServiceError, AccessTokenResponseBody]] = {

    val request = POST(
      AccessTokenRequestBody().asJson,
      baseUri / "oauth/token",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[AccessTokenResponseBody](request, client)
      .mapExceptionToServiceError("post oauth/token")

  }
}
