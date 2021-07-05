package io.iohk.atala.prism.kycbridge.services

import monix.eval.Task
import org.http4s.Method._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.{MediaType, Uri}
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import cats.implicits._
import io.grpc.Status

import ServiceUtils._
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.config.IdentityMindConfig
import io.iohk.atala.prism.kycbridge.services.IdentityMindService.{IdentityMindResponse, IdentityMindServiceError}
import io.iohk.atala.prism.kycbridge.models.identityMind.{ConsumerRequest, ConsumerResponse}

trait IdentityMindService {
  def consumer(consumerRequest: ConsumerRequest): Task[IdentityMindResponse[ConsumerResponse]]
}

object IdentityMindService {
  type IdentityMindResponse[A] = Either[IdentityMindServiceError, A]

  case class IdentityMindServiceError(methodName: String, throwable: Throwable) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Error occurred when calling Acuant IdentityMind service method: $methodName, cause: ${throwable.getMessage}"
      )
    }
  }

}

class IdentityMindServiceImpl(identityMindConfig: IdentityMindConfig, client: Client[Task])
    extends IdentityMindService
    with Http4sClientDsl[Task] {

  private implicit class IdentityMindServiceErrorOps[A](value: Task[Either[Exception, A]]) {
    def mapExceptionToServiceError(methodName: String): Task[Either[IdentityMindServiceError, A]] =
      value.map(_.leftMap(e => IdentityMindServiceError(methodName, e)))
  }

  private val baseUri = Uri.unsafeFromString(identityMindConfig.url)

  private lazy val authorization = basicAuthorization(identityMindConfig.password, identityMindConfig.password)

  override def consumer(
      consumerRequest: ConsumerRequest
  ): Task[IdentityMindResponse[ConsumerResponse]] = {
    val request = POST(
      consumerRequest.asJson,
      baseUri / "im" / "account" / "consumer",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[ConsumerResponse](request, client)
      .mapExceptionToServiceError("post document")
  }

}
