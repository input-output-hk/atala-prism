package io.iohk.atala.prism.management.console

import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential}
import io.iohk.atala.prism.protos.console_api

trait ManagementConsoleTestUtil {
  self: ManagementConsoleRpcSpecBase =>

  def checkContactExists(
      keyPair: ECKeyPair,
      did: DID,
      contact: Contact
  ): Boolean = {
    val getRequest = console_api.GetContactRequest(
      contactId = contact.contactId.toString
    )
    val getRpcRequest = SignedRpcRequest.generate(keyPair, did, getRequest)

    usingApiAsContacts(getRpcRequest) { serviceStub =>
      val response = serviceStub.getContact(getRequest)
      response.contact.exists(_.contactId == contact.contactId.toString)
    }
  }

  def checkCredentialExists(
      keyPair: ECKeyPair,
      did: DID,
      contact: Contact,
      credential: GenericCredential
  ): Boolean = {
    val getCredentialsRequest = console_api.GetContactCredentialsRequest(
      contactId = contact.contactId.toString
    )
    val getCredentialsRpcRequest =
      SignedRpcRequest.generate(keyPair, did, getCredentialsRequest)
    usingApiAsCredentials(getCredentialsRpcRequest) { serviceStub =>
      val response = serviceStub.getContactCredentials(getCredentialsRequest)
      response.genericCredentials
        .map(_.credentialId)
        .contains(credential.credentialId.toString)
    }
  }
}
