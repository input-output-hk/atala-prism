package io.iohk.node.poc

import java.time.LocalDate
import java.util.UUID

import com.github.ghik.silencer.silent
import io.iohk.prism.protos.cmanager_models.CManagerGenericCredential
import io.iohk.prism.protos.node_api
import io.iohk.prism.protos.node_api.{IssuerCredentialRequest, RevokeCredentialRequest}
import io.iohk.prism.protos.node_models.SignedAtalaOperation

case class CManager(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {

  // an example credentials we have from the backend
  def getCredential(): CManagerGenericCredential = {
    CManagerGenericCredential(
      credentialId = UUID.randomUUID().toString,
      issuerId = UUID.randomUUID().toString,
      subjectId = UUID.randomUUID().toString,
      credentialData = s"""{
           | "title" : "Bs in Computer Science",
           | "enrollmentDate" : "${LocalDate.now()}",
           | "graduationDate" : "${LocalDate.now()}",
           | "subjectName" : "Asymptomatic Joe"
           |}""".stripMargin,
      groupName = "Graduation COVID-19",
      issuerName = "National University of Rosario"
    )
  }

  // this is a toy API to simulate what the CManager would do
  // NOTE: If needed we could have a batched version of this method to issue a batch of credentials at once
  //       but we would need to wait for the batching release
  @silent("never used")
  def issueCredential(credentialId: String, issueCredentialOperation: SignedAtalaOperation): String = {
    // First some storage stuff to mark a credential as stored
    // It then send posts the operation to the node
    node.issueCredential(IssuerCredentialRequest(Some(issueCredentialOperation))).id
    // TODO: Define what component is responsible to get the credential published.
    //       We are currently assuming a happy path where the credential will get on-chain
    //       Note that the same situation happens with DID registration
  }

  @silent("never used")
  def revokeCredential(credentialId: String, revokeCredentialOperation: SignedAtalaOperation): Unit = {
    // First storage stuff
    // then, posting things on the blockchain through the node
    node.revokeCredential(RevokeCredentialRequest(Some(revokeCredentialOperation)))
    // similar TODOs as the method above
    ()
  }
}
