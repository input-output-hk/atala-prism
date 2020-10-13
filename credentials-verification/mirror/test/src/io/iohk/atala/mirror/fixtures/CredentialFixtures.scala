package io.iohk.atala.mirror.fixtures

import com.google.protobuf.ByteString

import io.iohk.prism.protos.credential_models.Credential

trait CredentialFixtures {

  val rawMessage: ByteString = createRawMessage("{}")

  def createRawMessage(json: String): ByteString = {
    Credential(typeId = "VerifiableCredential/RedlandIdCredential", credentialDocument = json).toByteString
  }

}
