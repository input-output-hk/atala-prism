package io.iohk.atala.cvp.webextension

import cats.syntax.functor._
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor}
import io.iohk.atala.cvp.webextension.util.NullableOps._
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.{
  BatchWasRevoked,
  CredentialBatchId,
  CredentialBatchIdCompanion,
  CredentialWasRevoked,
  InvalidMerkleProof,
  InvalidSignature,
  KeyWasNotValid,
  KeyWasRevoked,
  TimestampInfo,
  VerificationException
}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.{SHA256Digest, SHA256DigestCompanion}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.extras.{toLong, toNumber}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.{DID, DIDCompanion}

package object circe {
  implicit val didEncoder: Encoder[DID] = Encoder[String].contramap[DID](_.value)
  implicit val didDecoder: Decoder[DID] = Decoder[String].map[DID](DIDCompanion.fromString)

  implicit val credentialBatchIdEncoder: Encoder[CredentialBatchId] =
    Encoder[String].contramap[CredentialBatchId](_.id)
  implicit val credentialBatchIdDecoder: Decoder[CredentialBatchId] =
    Decoder[String].map[CredentialBatchId](s =>
      CredentialBatchIdCompanion.fromString(s).getNullable(throw new RuntimeException("Invalid credential batch ID"))
    )

  implicit val sHA256DigestEncoder: Encoder[SHA256Digest] = Encoder[String].contramap(_.hexValue())
  implicit val sHA256DigestDecoder: Decoder[SHA256Digest] = Decoder[String].map(SHA256DigestCompanion.fromHex)

  implicit val timestampInfoEncoder: Encoder[TimestampInfo] = Encoder[(Long, Double, Double)].contramap { t =>
    (toNumber(t.atalaBlockTimestamp).toLong, t.atalaBlockSequenceNumber, t.operationSequenceNumber)
  }
  implicit val timestampInfoDecoder: Decoder[TimestampInfo] = Decoder[(Long, Double, Double)].map {
    case (atalaBlockTimestamp, atalaBlockSequenceNumber, operationSequenceNumber) =>
      new TimestampInfo(toLong(atalaBlockTimestamp.toDouble), atalaBlockSequenceNumber, operationSequenceNumber)
  }

  implicit val batchWasRevokedEncoder: Encoder[BatchWasRevoked] = Encoder[TimestampInfo].contramap(_.revokedOn)
  implicit val batchWasRevokedDecoder: Decoder[BatchWasRevoked] = Decoder[TimestampInfo].map(new BatchWasRevoked(_))
  implicit val credentialWasRevokedEncoder: Encoder[CredentialWasRevoked] =
    Encoder[TimestampInfo].contramap(_.revokedOn)
  implicit val credentialWasRevokedDecoder: Decoder[CredentialWasRevoked] =
    Decoder[TimestampInfo].map(new CredentialWasRevoked(_))
  implicit val keyWasNotValidEncoder: Encoder[KeyWasNotValid] = Encoder[(TimestampInfo, TimestampInfo)].contramap { t =>
    (t.keyAddedOn, t.credentialIssuedOn)
  }
  implicit val keyWasNotValidDecoder: Decoder[KeyWasNotValid] = Decoder[(TimestampInfo, TimestampInfo)].map {
    case (keyAddedOn, credentialIssuedOn) =>
      new KeyWasNotValid(keyAddedOn, credentialIssuedOn)
  }
  implicit val keyWasRevokedEncoder: Encoder[KeyWasRevoked] = Encoder[(TimestampInfo, TimestampInfo)].contramap { t =>
    (t.credentialIssuedOn, t.keyRevokedOn)
  }
  implicit val keyWasRevokedDecoder: Decoder[KeyWasRevoked] = Decoder[(TimestampInfo, TimestampInfo)].map {
    case (credentialIssuedOn, keyRevokedOn) =>
      new KeyWasRevoked(credentialIssuedOn, keyRevokedOn)
  }
  val invalidMerkleProofJson = "{\"type\": \"InvalidMerkleProof\"}".asJson
  val invalidSignatureJson = "{\"type\": \"InvalidSignature\"}".asJson

  implicit val verificationExceptionEncoder: Encoder[VerificationException] = Encoder.instance {
    case batchWasRevoked: BatchWasRevoked => batchWasRevoked.asJson
    case credentialWasRevoked: CredentialWasRevoked => credentialWasRevoked.asJson
    case `InvalidMerkleProof` => invalidMerkleProofJson
    case keyWasNotValid: KeyWasNotValid => keyWasNotValid.asJson
    case keyWasRevoked: KeyWasRevoked => keyWasRevoked.asJson
    case `InvalidSignature` => invalidSignatureJson
    case _ => throw new IllegalStateException("Unknown VerificationException type")
  }

  implicit val verificationExceptionDecoder: Decoder[VerificationException] =
    List[Decoder[VerificationException]](
      Decoder[BatchWasRevoked].widen,
      Decoder[CredentialWasRevoked].widen,
      Decoder[KeyWasNotValid].widen,
      Decoder[KeyWasRevoked].widen,
      (c: HCursor) =>
        c.value match {
          case `invalidMerkleProofJson` => Right(InvalidMerkleProof)
          case `invalidSignatureJson` => Right(InvalidSignature)
          case _ => Left(DecodingFailure("Not a valid VerificationException type", List.empty))
        }
    ).reduceLeft(_ or _)
}
