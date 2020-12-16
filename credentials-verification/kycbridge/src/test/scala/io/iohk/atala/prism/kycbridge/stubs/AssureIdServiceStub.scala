package io.iohk.atala.prism.kycbridge.stubs

import io.iohk.atala.prism.kycbridge.models.assureId.{Device, NewDocumentInstanceResponseBody}
import io.iohk.atala.prism.kycbridge.services.AssureIdService
import monix.eval.Task

class AssureIdServiceStub(newDocumentInstanceResponse: Either[Exception, NewDocumentInstanceResponseBody])
    extends AssureIdService {

  def createNewDocumentInstance(device: Device): Task[Either[Exception, NewDocumentInstanceResponseBody]] =
    Task.pure(newDocumentInstanceResponse)

  def getDocumentStatus(id: String): Task[Either[Exception, Int]] = {
    Task.pure(Right(0))
  }

}
