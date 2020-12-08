package io.iohk.atala.prism.kycbridge.services

import io.iohk.atala.prism.kycbridge.config.AcuantConfig
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
import org.http4s.{AuthScheme, Credentials, MediaType, Uri}
import io.circe.syntax._
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import org.http4s.circe._
import ServiceUtils._

trait AssureIdService {
  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]]
  def getDocumentStatus(id: String, bearerToken: String): Task[Either[Exception, Int]]
}

class AssureIdServiceImpl(acuantConfig: AcuantConfig, client: Client[Task])
    extends AssureIdService
    with Http4sClientDsl[Task] {

  private val baseUri = Uri.unsafeFromString(acuantConfig.assureIdUrl)

  private lazy val authorization = basicAuthorization(acuantConfig)

  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]] = {
    val newDocumentInstanceRequestBody = NewDocumentInstanceRequestBody(
      device = device,
      subscriptionId = acuantConfig.subscriptionId
    )

    val request = POST(
      newDocumentInstanceRequestBody.asJson,
      baseUri / "AssureIDService.svc/Document/Instance",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[String](request, client).map(result => result.map(NewDocumentInstanceResponseBody))
  }

  //bearer token usage example
  def getDocumentStatus(id: String, bearerToken: String): Task[Either[Exception, Int]] = {
    val request = GET(
      baseUri / s"AssureIDService.svc/Document/$id/Status",
      Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken)),
      Accept(MediaType.application.json)
    )

    runRequestToEither[Int](request, client)
  }
}
