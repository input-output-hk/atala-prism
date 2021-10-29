package io.iohk.atala.prism.node.poc.batch

import java.time.LocalDate
import java.util.UUID
import io.iohk.atala.prism.protos.console_models.CManagerGenericCredential
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.{
  IssueCredentialBatchRequest,
  IssueCredentialBatchResponse,
  RevokeCredentialsRequest,
  RevokeCredentialsResponse
}
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
  ): IssueCredentialBatchResponse = {
    // First some storage stuff to mark a credential as stored
    // It then posts the operation to the node
    node.issueCredentialBatch(
      IssueCredentialBatchRequest(Some(issueCredentialBatchOperation))
    )
  }

  def revokeCredentialBatch(
      revokeCredentialBatchOperation: SignedAtalaOperation
  ): RevokeCredentialsResponse = {
    // First storage stuff
    // then, posting things on the blockchain through the node
    node.revokeCredentials(
      RevokeCredentialsRequest(Some(revokeCredentialBatchOperation))
    )
  }

  def revokeSpecificCredentials(
      revokeCredentialBatchOperation: SignedAtalaOperation
  ): RevokeCredentialsResponse = {
    // First storage stuff
    // then, posting things on the blockchain through the node
    node.revokeCredentials(
      RevokeCredentialsRequest(Some(revokeCredentialBatchOperation))
    )
  }
}
