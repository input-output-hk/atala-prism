package io.iohk.atala.prism.kycbridge.services

import monix.eval.Task
import org.http4s.Method._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.{MediaType, Uri}
import io.circe.syntax._
import org.http4s.circe._
import ServiceUtils._

import io.iohk.atala.prism.kycbridge.config.AcuantConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{
  Device,
  NewDocumentInstanceRequestBody,
  NewDocumentInstanceResponseBody,
  Document,
  DocumentStatus
}
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._

trait AssureIdService {
  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]]
  def getDocument(id: String): Task[Either[Exception, Document]]
  def getDocumentStatus(id: String): Task[Either[Exception, DocumentStatus]]
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

  def getDocument(id: String): Task[Either[Exception, Document]] = {
    val request = GET(
      baseUri / "AssureIDService.svc/Document" / id,
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[Document](request, client)
  }

  def getDocumentStatus(id: String): Task[Either[Exception, DocumentStatus]] = {
    val request = GET(
      baseUri / "AssureIDService.svc/Document" / id / "Status",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[Int](request, client).map(_.flatMap(DocumentStatus.fromInt))
  }
}
