package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.assureId.{Device, DocumentStatus, NewDocumentInstanceResponseBody}
import io.iohk.atala.prism.kycbridge.services.AssureIdService
import monix.eval.Task
import io.iohk.atala.prism.kycbridge.models.assureId.Document

class AssureIdServiceStub(
    newDocumentInstanceResponse: Either[Exception, NewDocumentInstanceResponseBody] = Right(
      NewDocumentInstanceResponseBody(documentId = "documentId")
    ),
    document: Either[Exception, Document] = Right(
      Document(instanceId = "id", biographic = None, classification = None)
    ),
    documentStatus: Either[Exception, DocumentStatus] = Right(DocumentStatus.None)
) extends AssureIdService {

  def getDocument(id: String): Task[Either[Exception, Document]] =
    Task.pure(document)

  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]] =
    Task.pure(newDocumentInstanceResponse)

  def getDocumentStatus(id: String): Task[Either[Exception, DocumentStatus]] =
    Task.pure(documentStatus)

  def getFrontImageFromDocument(id: String): Task[Either[Exception, Array[Byte]]] = {
    Task.pure(Right(Array()))
  }

}
