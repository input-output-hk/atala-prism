package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DocumentStatus, NewDocumentInstanceResponseBody}
import io.iohk.atala.prism.kycbridge.services.AssureIdService
import monix.eval.Task
import io.iohk.atala.prism.kycbridge.models.assureId.Document
import io.iohk.atala.prism.kycbridge.services.AssureIdService.AssureIdServiceError

class AssureIdServiceStub(
    newDocumentInstanceResponse: Either[AssureIdServiceError, NewDocumentInstanceResponseBody] = Right(
      NewDocumentInstanceResponseBody(documentId = "documentId")
    ),
    document: Either[AssureIdServiceError, Document] = Right(
      Document(instanceId = "id", biographic = None, classification = None, dataFields = None)
    ),
    documentStatus: Either[AssureIdServiceError, DocumentStatus] = Right(DocumentStatus.None),
    getFrontImageFromDocumentResponse: Either[AssureIdServiceError, Array[Byte]] = Right(Array.empty),
    getImageFromDocumentResponse: Either[AssureIdServiceError, Array[Byte]] = Right(Array.empty)
) extends AssureIdService {

  def getDocument(id: String): Task[Either[AssureIdServiceError, Document]] =
    Task.pure(document)

  def createNewDocumentInstance(device: Device): Task[Either[AssureIdServiceError, NewDocumentInstanceResponseBody]] =
    Task.pure(newDocumentInstanceResponse)

  def getDocumentStatus(id: String): Task[Either[AssureIdServiceError, DocumentStatus]] =
    Task.pure(documentStatus)

  def getFrontImageFromDocument(id: String): Task[Either[AssureIdServiceError, Array[Byte]]] =
    Task.pure(getFrontImageFromDocumentResponse)

  def getImageFromDocument(id: String, side: String): Task[Either[AssureIdServiceError, Array[Byte]]] =
    Task.pure(getImageFromDocumentResponse)

}
