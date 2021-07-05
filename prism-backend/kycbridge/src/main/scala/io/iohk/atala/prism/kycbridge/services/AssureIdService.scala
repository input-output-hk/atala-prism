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
import io.grpc.Status
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.kycbridge.config.AcuantConfig
import io.iohk.atala.prism.kycbridge.models.assureId.{
  Device,
  Document,
  DocumentStatus,
  NewDocumentInstanceRequestBody,
  NewDocumentInstanceResponseBody
}
import io.iohk.atala.prism.kycbridge.models.assureId.implicits._
import io.iohk.atala.prism.kycbridge.services.AssureIdService.AssureIdServiceError
import cats.implicits._

trait AssureIdService {
  def createNewDocumentInstance(device: Device): Task[Either[AssureIdServiceError, NewDocumentInstanceResponseBody]]
  def getDocument(id: String): Task[Either[AssureIdServiceError, Document]]
  def getDocumentStatus(id: String): Task[Either[AssureIdServiceError, DocumentStatus]]
  def getFrontImageFromDocument(id: String): Task[Either[AssureIdServiceError, Array[Byte]]]
  def getImageFromDocument(id: String, side: String): Task[Either[AssureIdServiceError, Array[Byte]]]
}

object AssureIdService {
  case class AssureIdServiceError(methodName: String, throwable: Throwable) extends PrismError {
    override def toStatus: Status = {
      Status.INTERNAL.withDescription(
        s"Error occurred when calling Acuant AssureId service method: $methodName, cause: ${throwable.getMessage}"
      )
    }
  }

}

class AssureIdServiceImpl(acuantConfig: AcuantConfig, client: Client[Task])
    extends AssureIdService
    with Http4sClientDsl[Task] {

  private implicit class AssureIdServiceErrorOps[A](value: Task[Either[Exception, A]]) {
    def mapExceptionToServiceError(methodName: String): Task[Either[AssureIdServiceError, A]] =
      value.map(_.leftMap(e => AssureIdServiceError(methodName, e)))
  }

  private val baseUri = Uri.unsafeFromString(acuantConfig.assureIdUrl)

  private lazy val authorization = basicAuthorization(acuantConfig)

  def createNewDocumentInstance(device: Device): Task[Either[AssureIdServiceError, NewDocumentInstanceResponseBody]] = {
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

    runRequestToEither[String](request, client)
      .map(result => result.map(NewDocumentInstanceResponseBody))
      .mapExceptionToServiceError("post document")
  }

  def getDocument(id: String): Task[Either[AssureIdServiceError, Document]] = {
    val request = GET(
      baseUri / "AssureIDService.svc/Document" / id,
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[Document](request, client)
      .mapExceptionToServiceError("get document")
  }

  def getDocumentStatus(id: String): Task[Either[AssureIdServiceError, DocumentStatus]] = {
    val request = GET(
      baseUri / "AssureIDService.svc/Document" / id / "Status",
      authorization,
      Accept(MediaType.application.json)
    )

    runRequestToEither[Int](request, client)
      .map(_.flatMap(DocumentStatus.fromInt))
      .mapExceptionToServiceError("get status")
  }

  def getFrontImageFromDocument(id: String): Task[Either[AssureIdServiceError, Array[Byte]]] = {
    val request = GET(
      (baseUri / "AssureIDService.svc/Document" / id / "Field/Image").+?("key", "Photo"),
      authorization
    )

    fetchBinaryData(request, client).map(_.leftMap(e => AssureIdServiceError("get photo", e)))
  }

  def getImageFromDocument(id: String, side: String): Task[Either[AssureIdServiceError, Array[Byte]]] = {
    val request = GET(
      (baseUri / "AssureIDService.svc/Document" / id / "Image").+?("side", side),
      authorization
    )

    fetchBinaryData(request, client).map(_.leftMap(e => AssureIdServiceError(s"get $side image", e)))
  }
}
