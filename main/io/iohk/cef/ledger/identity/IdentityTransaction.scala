package io.iohk.cef.ledger.identity

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto.certificates.CachedCertificatePair
import io.iohk.cef.crypto.{sign => signBytes, _}
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[IdentityData] {
  val data: IdentityTransactionData
  val signature: Signature
}

object IdentityTransaction {

  def sign(data: IdentityTransactionData, privateKey: SigningPrivateKey): Signature = {
    signBytes(data, privateKey)
  }

  def isDataSignedWith[D <: IdentityTransactionData: NioCodec](
      data: D,
      publicKey: SigningPublicKey,
      signature: Signature
  ): Boolean =
    isValidSignature(data, signature, publicKey)

  def isDataSignedWithIdentity[D <: IdentityTransactionData: NioCodec](
      data: D,
      identity: Identity,
      state: IdentityLedgerState,
      signature: Signature
  ): Boolean = {
    // TODO: Go directly to the expected key
    state
      .get(identity)
      .map(_.keys)
      .getOrElse(Set.empty)
      .exists(isDataSignedWith(data, _, signature))
  }

  def grantingAuthorities: Set[Identity] = Set()
}

case class Claim(data: ClaimData, signature: Signature) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    if (!isValidSignature(data, signature, data.key)) {
      Left(UnableToVerifySignatureError)
    } else if (ledgerState.contains(data.identity)) {
      Left(IdentityTakenError(data.identity))
    } else {
      Right(ledgerState.put(data.identity, IdentityData.forKeys(data.key)))
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

object Claim {
  def apply(data: ClaimData, privateKey: SigningPrivateKey): Claim = {
    Claim(data, signBytes(data, privateKey))
  }
}

/**
  * A transaction to link a new key with an existing identity.
  *
  * @param identity the identity where the new key is linked
  * @param key the public key to link to the given identity
  * @param signature a digital signature validating the transaction, it should be generated
  *                  from one of the existing keys on the given identity.
  * @param linkingIdentitySignature a digital signature validating the transaction, it should be generated
  *   *                  from the public key on the given identity.
  */
case class Link(data: LinkData, signature: Signature, linkingIdentitySignature: Signature) extends IdentityTransaction {

  /**
    * Apply this transaction to the given state
    *
    * @param ledgerState the state
    * @return the new state on success or an error if:
    *         - the identity hasn't ben claimed
    *         - the signature can't be validated
    */
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    lazy val providedPublicKeySignatureValid =
      isValidSignature(data, linkingIdentitySignature, data.key)
    lazy val identitySignatureValid =
      IdentityTransaction.isDataSignedWithIdentity(data, data.identity, ledgerState, signature)

    if (!ledgerState.contains(data.identity)) {
      Left(IdentityNotClaimedError(data.identity))
    } else if (!identitySignatureValid) {
      Left(UnableToVerifySignatureError)
    } else if (!providedPublicKeySignatureValid) {
      Left(UnableToVerifyLinkingIdentitySignatureError(data.identity, data.key))
    } else {
      val prev: IdentityData = ledgerState.get(data.identity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.identity, prev addKey data.key)
      Right(result)
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

object Link {
  def apply(
      data: LinkData,
      identityPrivateKey: SigningPrivateKey,
      providedKeyPrivateCounterpart: SigningPrivateKey): Link =
    Link(data, signBytes(data, identityPrivateKey), signBytes(data, providedKeyPrivateCounterpart))
}

case class Unlink(data: UnlinkData, signature: Signature) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    if (!IdentityTransaction.isDataSignedWithIdentity(data, data.identity, ledgerState, signature)) {
      Left(UnableToVerifySignatureError)
    } else if (!ledgerState.contains(data.identity) || !ledgerState
        .get(data.identity)
        .map(_.keys)
        .getOrElse(Set())
        .contains(data.key)) {
      Left(PublicKeyNotAssociatedWithIdentity(data.identity, data.key))
    } else {
      val prev: IdentityData = ledgerState.get(data.identity).getOrElse(IdentityData.empty)
      val newData = prev removeKey data.key
      if (newData.isEmpty) {
        Right(ledgerState.remove(data.identity))
      } else {
        Right(ledgerState.put(data.identity, newData))
      }
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

object Unlink {
  def apply(data: UnlinkData, privateKey: SigningPrivateKey): Unlink =
    Unlink(data, signBytes(data, privateKey))
}

/**
  *  A transaction to endorse a already claimed identity.
  * @param identity endorser's identity should be already claimed identity
  * @param key PublicKey used by endorser signature.
  * @param signature endorser's digital signature validating the transaction.
  * @param endorsedIdentity identity to endorse should be already claimed identity.
  */
case class Endorse(data: EndorseData, signature: Signature) extends IdentityTransaction {

  def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    if (!ledgerState.contains(data.endorsedIdentity)) {
      Left(UnknownEndorsedIdentityError(data.endorsedIdentity))
    } else if (!ledgerState.contains(data.endorserIdentity)) {
      Left(UnknownEndorserIdentityError(data.endorserIdentity))
    } else if (!IdentityTransaction.isDataSignedWithIdentity(data, data.endorserIdentity, ledgerState, signature)) {
      Left(UnableToVerifyEndorserSignatureError(data.endorserIdentity, signature))
    } else {
      val prev: IdentityData = ledgerState.get(data.endorsedIdentity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.endorsedIdentity, prev endorse data.endorserIdentity)
      Right(result)
    }
  }

  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[io.iohk.cef.ledger.LedgerState]] for more detail.
    *
    * @return Set[String]
    */
  override def partitionIds: Set[String] = Set(data.endorserIdentity, data.endorsedIdentity)
}

object Endorse {
  def apply(data: EndorseData, endorserIdentityPrivateKey: SigningPrivateKey): Endorse =
    Endorse(data, signBytes(data, endorserIdentityPrivateKey))
}

/**
  *  A transaction to revoke  an already endorsed identity.
  * @param identity endorser's identity should be already claimed identity
  * @param key PublicKey used by endorser signature.
  * @param signature endorser's digital signature validating the transaction.
  * @param endorsedIdentity identity to revoke should be already endorsed identity.
  */
case class RevokeEndorsement(data: RevokeEndorsementData, signature: Signature) extends IdentityTransaction {

  def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    lazy val validEndorsement: Boolean = ledgerState
      .get(data.endorsedIdentity)
      .map(_.endorsers)
      .getOrElse(Set.empty)
      .contains(data.endorserIdentity)

    if (!ledgerState.contains(data.endorserIdentity)) {
      Left(UnknownEndorserIdentityError(data.endorserIdentity))
    } else if (!ledgerState.contains(data.endorsedIdentity)) {
      Left(UnknownEndorsedIdentityError(data.endorsedIdentity))
    } else if (!IdentityTransaction.isDataSignedWithIdentity(data, data.endorserIdentity, ledgerState, signature)) {
      Left(UnableToVerifyEndorserSignatureError(data.endorserIdentity, signature))
    } else if (!validEndorsement) {
      Left(EndorsementNotAssociatedWithIdentityError(data.endorserIdentity, data.endorsedIdentity))
    } else {
      val prev: IdentityData = ledgerState.get(data.endorsedIdentity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.endorsedIdentity, prev revoke data.endorserIdentity)
      Right(result)
    }
  }

  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[io.iohk.cef.ledger.LedgerState]] for more detail.
    *
    * @return Set[String]
    */
  override def partitionIds: Set[String] = Set(data.endorserIdentity, data.endorsedIdentity)
}

object RevokeEndorsement {
  def apply(data: RevokeEndorsementData, endorsingIdentityPrivateKey: SigningPrivateKey): RevokeEndorsement =
    RevokeEndorsement(data, signBytes(data, endorsingIdentityPrivateKey))
}

case class Grant(data: GrantData, signature: Signature, claimSignature: Signature, endorseSignature: Signature)
    extends IdentityTransaction {
  private val underlyingClaim = Claim(data.underlyingClaimData, claimSignature)
  private val underlyingEndorse = Endorse(data.underlyingEndorseData, endorseSignature)
  val txs: Seq[Transaction[IdentityData]] = Seq(underlyingClaim, underlyingEndorse)
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    if (!IdentityTransaction.grantingAuthorities.contains(data.grantingIdentity)) {
      Left(IdentityIsNotAGrantingAuthorityError(data.grantingIdentity))
    } else {
      txs.foldLeft[Either[LedgerError, IdentityLedgerState]](Right(ledgerState))(
        (stateEither, tx) => stateEither.flatMap(state => tx(state))
      )
    }
  }
  override def partitionIds: Set[String] =
    txs.foldLeft(Set(data.grantingIdentity))(_ union _.partitionIds)
}

object Grant {
  def apply(
      data: GrantData,
      grantingIdentityPrivateKey: SigningPrivateKey,
      grantedIdentityPrivateKey: SigningPrivateKey): Grant = {
    val grantSignature = signBytes(data, grantingIdentityPrivateKey)
    val claimSignature = signBytes(data.underlyingClaimData, grantedIdentityPrivateKey)
    val endorseSignature = signBytes(data.underlyingEndorseData, grantingIdentityPrivateKey)
    Grant(data, grantSignature, claimSignature, endorseSignature)
  }
}

/**
  * The following signatures are required:
  * - The linking identity must sign with a secret key associated with one of their stored public keys, this ensures that
  *    the linking identity has authorized the transaction.
  * - The linking identity must sign with the secret key that associated with its public key from the certificate, this
  *    ensures that it could use the certificate key.
  * - The certificate issuer signs the linking certificate with a secret key that is linked to its identity, this ensures
  *    that the issuer is actually allowed to endorse the linking identity.
  *
  * @param data the transaction data
  * @param signature signature using the secret key associated with a public key that belongs to the linking linkingIdentity
  * @param signatureFromCertificate signature using secret key associated with the public key of the linking certificate
  */
case class LinkCertificate(data: LinkCertificateData, signature: Signature, signatureFromCertificate: Signature)
    extends IdentityTransaction {

  private val decodedPairResult = CachedCertificatePair.decode(data.pem)

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    decodedPairResult
      .map { apply(_, ledgerState) }
      .getOrElse { Left(InvalidCertificateError) }
  }

  override val partitionIds: Set[String] = {
    decodedPairResult
      .map { pair =>
        Set(pair.target.identity, pair.issuer.identity)
      }
      .getOrElse(Set.empty)
  }

  private def apply(
      pair: CachedCertificatePair,
      ledgerState: IdentityLedgerState
  ): Either[LedgerError, IdentityLedgerState] = {

    val authorityIdentityDataMaybe = ledgerState.get(pair.issuer.identity)
    val linkingIdentityDataMaybe = ledgerState.get(pair.target.identity)

    val keyBelongsToAuthority = authorityIdentityDataMaybe
      .map(_.keys)
      .getOrElse(Set.empty)
      .contains(pair.issuer.publicKey)

    lazy val linkingCertificateSignatureValid =
      IdentityTransaction.isDataSignedWith(data, pair.target.publicKey, signatureFromCertificate)

    lazy val existingKeySignatureValid =
      IdentityTransaction.isDataSignedWithIdentity(data, pair.target.identity, ledgerState, signature)

    lazy val certificateSignatureValid = pair.isSignatureValid

    (authorityIdentityDataMaybe, linkingIdentityDataMaybe) match {
      case (None, _) => Left(IdentityNotClaimedError(pair.issuer.identity))
      case (_, None) => Left(IdentityNotClaimedError(pair.target.identity))

      case _ if data.linkingIdentity != pair.target.identity =>
        Left(IdentityNotMatchingCertificate(data.linkingIdentity, pair.target.identity))

      case _ if !keyBelongsToAuthority =>
        Left(PublicKeyNotAssociatedWithIdentity(pair.issuer.identity, pair.issuer.publicKey))

      case _ if !certificateSignatureValid =>
        Left(UnableToVerifyLinkingIdentitySignatureError(pair.issuer.identity, pair.issuer.publicKey))

      case _ if !existingKeySignatureValid =>
        Left(UnableToVerifySignatureError)

      case _ if !linkingCertificateSignatureValid =>
        Left(UnableToVerifyLinkingIdentitySignatureError(pair.target.identity, pair.target.publicKey))

      case (Some(_), Some(linkingIdentityData)) =>
        val newIdentity = linkingIdentityData
          .addKey(pair.target.publicKey)
          .endorse(pair.issuer.identity)

        val result = ledgerState.put(pair.target.identity, newIdentity)
        Right(result)
    }
  }
}

object LinkCertificate {
  def apply(
      data: LinkCertificateData,
      existingKey: SigningPrivateKey,
      certificateKey: SigningPrivateKey): LinkCertificate = {
    LinkCertificate(
      data = data,
      signature = signBytes(data, existingKey),
      signatureFromCertificate = signBytes(data, certificateKey)
    )
  }
}
