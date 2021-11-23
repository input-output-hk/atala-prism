package io.iohk.atala.prism.node.poc.batch

import java.time.LocalDate
import java.util.UUID
import io.iohk.atala.prism.protos.console_models.CManagerGenericCredential
import io.iohk.atala.prism.protos.node_api.ScheduleOperationsRequest
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class ManagementConsole(
    node: node_api.NodeServiceGrpc.NodeServiceBlockingStub
) {
  // example credentials we have from the backend
  def getCredentials(
      amountOfCredentials: Int
  ): List[CManagerGenericCredential] =
    (1 to amountOfCredentials).toList.map { index =>
      CManagerGenericCredential(
        credentialId = UUID.randomUUID().toString,
        issuerId = UUID.randomUUID().toString,
        contactId = UUID.randomUUID().toString,
        credentialData = s"""{
             | "title" : "Bs in Computer Science",
             | "enrollmentDate" : "${LocalDate.now()}",
             | "graduationDate" : "${LocalDate.now()}",
             | "subjectName" : "Asymptomatic Joe $index"
             |}""".stripMargin,
        issuerName = "National University of Rosario"
      )
    }

  // this is a toy API to simulate what the console does
  def issueCredentialBatch(
      issueCredentialBatchOperation: SignedAtalaOperation
  ): node_models.OperationOutput = {
    // First some storage stuff to mark a credential as stored
    // It then posts the operation to the node
    node
      .scheduleOperations(
        ScheduleOperationsRequest(List(issueCredentialBatchOperation))
      )
      .outputs
      .head
  }

  def revokeCredentialBatch(
      revokeCredentialBatchOperation: SignedAtalaOperation
  ): node_models.OperationOutput = {
    // First storage stuff
    // then, posting things on the blockchain through the node
    node
      .scheduleOperations(
        ScheduleOperationsRequest(List(revokeCredentialBatchOperation))
      )
      .outputs
      .head
  }

  def revokeSpecificCredentials(
      revokeCredentialBatchOperation: SignedAtalaOperation
  ): node_models.OperationOutput = {
    // First storage stuff
    // then, posting things on the blockchain through the node
    node
      .scheduleOperations(
        ScheduleOperationsRequest(List(revokeCredentialBatchOperation))
      )
      .outputs
      .head
  }
}
