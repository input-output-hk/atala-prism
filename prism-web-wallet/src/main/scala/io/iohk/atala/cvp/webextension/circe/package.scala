package io.iohk.atala.cvp.webextension

import io.circe.{Decoder, Encoder}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID

package object circe {
  implicit val didEncoder: Encoder[DID] = Encoder[String].contramap[DID](_.value)
  implicit val didDecoder: Decoder[DID] = Decoder[String].map[DID](s => DID.unsafeFromString(s))

  implicit val credentialBatchIdEncoder: Encoder[CredentialBatchId] = Encoder[String].contramap[CredentialBatchId](_.id)
  implicit val credentialBatchIdDecoder: Decoder[CredentialBatchId] = {
    Decoder[String].map[CredentialBatchId](s => CredentialBatchId.unsafeFromString(s))
  }

  implicit val sHA256DigestEncoder: Encoder[SHA256Digest] = Encoder[String].contramap(_.hexValue)
  implicit val sHA256DigestDecoder: Decoder[SHA256Digest] = Decoder[String].map(SHA256Digest.fromHex)
}
