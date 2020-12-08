package io.iohk.atala.prism.kycbridge.services

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.iohk.atala.prism.kycbridge.config.AssureIdConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{
  Device,
  NewDocumentInstanceRequestBody,
  NewDocumentInstanceResponseBody
}
import monix.eval.Task
import org.http4s.Method._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.{AuthScheme, Credentials, EntityDecoder, MediaType, Request, Status, Uri}
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import org.http4s.circe._

trait AssureIdService {
  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]]
}

class AssureIdServiceImpl(assureIdConfig: AssureIdConfig, client: Client[Task])
    extends AssureIdService
    with Http4sClientDsl[Task] {

  private val baseUri = Uri.unsafeFromString(assureIdConfig.url)

  private lazy val credentials = Base64.getEncoder
    .encodeToString(s"${assureIdConfig.username}:${assureIdConfig.password}".getBytes(StandardCharsets.UTF_8))

  private lazy val authorization = Authorization(Credentials.Token(AuthScheme.Basic, credentials))

  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]] = {
    val newDocumentInstanceRequestBody = NewDocumentInstanceRequestBody(
      device = device,
      subscriptionId = assureIdConfig.subscriptionId
    )

    val request = POST(
      newDocumentInstanceRequestBody.asJson,
      baseUri / "AssureIDService.svc/Document/Instance",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[String](request).map(result => result.map(NewDocumentInstanceResponseBody))
  }

  private def runRequestToEither[A](
      request: Task[Request[Task]]
  )(implicit entityDecoder: EntityDecoder[Task, A]): Task[Either[Exception, A]] = {
    request.flatMap(client.run(_).use {
      case Status.Successful(r) => r.attemptAs[A].value
      case r =>
        r.as[String]
          .map(b => Left(new Exception(s"Request failed with status ${r.status.code} ${r.status.reason} and body $b")))
    })
  }
}
