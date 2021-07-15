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
import io.iohk.atala.prism.kycbridge.models.identityMind._

trait IdentityMindService {
  def consumer(consumerRequest: PostConsumerRequest): Task[IdentityMindResponse[PostConsumerResponse]]
  def consumer(consumerRequest: GetConsumerRequest): Task[IdentityMindResponse[GetConsumerResponse]]
  def attributes(attributesRequest: AttributesRequest): Task[IdentityMindResponse[AttributesResponse]]
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
      consumerRequest: PostConsumerRequest
  ): Task[IdentityMindResponse[PostConsumerResponse]] = {
    val request = POST(
      consumerRequest.asJson,
      baseUri / "im" / "account" / "consumer",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[PostConsumerResponse](request, client)
      .mapExceptionToServiceError("post consumer")
  }

  override def consumer(consumerRequest: GetConsumerRequest): Task[IdentityMindResponse[GetConsumerResponse]] = {
    val request = GET(
      baseUri / "im" / "account" / "consumer" / "v2" / consumerRequest.mtid,
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[GetConsumerResponse](request, client)
      .mapExceptionToServiceError("get consumer")
  }

  override def attributes(attributesRequest: AttributesRequest): Task[IdentityMindResponse[AttributesResponse]] = {
    val request = GET(
      baseUri / "im" / "account" / "consumer" / attributesRequest.mtid / "attributes",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[AttributesResponse](request, client)
      .mapExceptionToServiceError("attributes")
  }

}
