package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.identity.DID

import scala.util.{Failure, Success, Try}

class SignedCredentialDetails private (
    val credential: SignedCredential,
    val issuerDID: DID,
    val issuanceKeyId: String,
    val slayerCredentialId: SlayerCredentialId
)

object SignedCredentialDetails {
  case class Error(msg: String)

  /**
    * Given the string representation for a signed credential, compute its associated details
    *
    * @param signedCredentialStringRepresentation the signed credential represented as a string
    * @return the associated details
    */
  def compute(signedCredentialStringRepresentation: String): Either[Error, SignedCredentialDetails] = {
    def f =
      Try {
        for {
          signedCredential <-
            SignedCredential
              .from(signedCredentialStringRepresentation)
              .toEither
              .left
              .map(e => Error(e.getMessage))

          unsignedCredential = signedCredential.decompose[JsonBasedUnsignedCredential].credential
          issuerDID <- unsignedCredential.issuerDID.toRight(Error("The credential doesn't include the issuerDID"))
          issuanceKeyId <-
            unsignedCredential.issuanceKeyId.toRight(Error("The credential doesn't include the issuanceKeyId"))

          slayerCredentialId = SlayerCredentialId.compute(
            credential = signedCredential,
            did = issuerDID
          )
        } yield new SignedCredentialDetails(
          credential = signedCredential,
          slayerCredentialId = slayerCredentialId,
          issuerDID = issuerDID,
          issuanceKeyId = issuanceKeyId
        )
      }

    f match {
      case Success(value) => value
      case Failure(ex) => Left(Error(ex.getMessage))
    }
  }
}
