package io.iohk.atala.credentials

import java.time.Instant

object CredentialVerification {

  /** This method receives data retrieved from the node and the credential to verify and
    * returns true if and only if the credential is valid.
    *
    * We have some assumptions to call this method:
    * 1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential
    * 2. The credentialData is obtained from the PRISM node and corresponds to the signedCredential parameter
    * 3. The issuer DID is a trusted one
    *
    * @param keyData the public key used to sign the credential and its addition and (optional)
    *                revocation timestamps
    * @param credentialData the credential information extracted from the node
    * @param signedCredential the credential to verify
    * @return true if and only if the credential is considered valid under the assumptions
    */
  def verifyCredential(
      keyData: KeyData,
      credentialData: CredentialData,
      signedCredential: SignedCredential
  ): Boolean = {
    // the credential is not revoked
    credentialData.revokedOn.isEmpty &&
    // the key was added before the credential was issued
    (keyData.addedOn occurredBefore credentialData.issuedOn) &&
    // the key is not revoked or, the key was revoked after the credential was signed
    keyData.revokedOn.fold(true)(credentialData.issuedOn.occurredBefore) &&
    // the signature is valid
    CredentialsCryptoSDKImpl.verifyCredentialSignature(signedCredential, keyData.publicKey)
  }
}
