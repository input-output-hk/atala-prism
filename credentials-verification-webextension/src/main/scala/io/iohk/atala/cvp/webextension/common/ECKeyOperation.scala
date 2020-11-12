package io.iohk.atala.cvp.webextension.common

import com.google.protobuf.ByteString
import io.circe.ParsingFailure
import io.iohk.atala.prism.credentials.{
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  UnsignedCredentialBuilder
}
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.util.BigIntOps
import typings.bip32.bip32Mod.BIP32Interface
import typings.bip32.{mod => bip32}
import typings.node.Buffer

object ECKeyOperation {
  val CURVE_NAME = "secp256k1"
  val firstMasterKeyId = "master0"

  private implicit val ec: ECTrait = EC
  // https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
  private val firstMasterChild = "m/0'/0'/0'"

  def didFromMasterKey(ecKeyPair: ECKeyPair): DID = {
    val atalaOperation = createDIDAtalaOperation(ecKeyPair)
    val didSuffix = SHA256Digest.compute(atalaOperation.toByteArray).hexValue
    DID.buildPrismDID(didSuffix)
  }

  def createDIDAtalaOperation(ecKeyPair: ECKeyPair): AtalaOperation = {
    val publicKey =
      toPublicKey(firstMasterKeyId, toECKeyData(ecKeyPair.publicKey), KeyUsage.MASTER_KEY)
    val didData = DIDData(publicKeys = Seq(publicKey))
    val createDIDOperation = CreateDIDOperation(Some(didData))
    val atalaOperation = AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOperation))
    atalaOperation
  }

  def issuerOperation(
      issuerDID: DID,
      signingKeyId: String,
      signingKey: ECKeyPair,
      claimsString: String
  ): Either[ParsingFailure, (AtalaOperation, String)] = {
    io.circe.parser.parse(claimsString) map { claims =>
      val unsignedCreedential =
        UnsignedCredentialBuilder[JsonBasedUnsignedCredential].buildFrom(
          issuerDID = issuerDID,
          issuanceKeyId = signingKeyId,
          claims = claims
        )
      val signedCredential = CredentialsCryptoSDKImpl.signCredential(unsignedCreedential, signingKey.privateKey)
      val contentHash = ByteString.copyFrom(CredentialsCryptoSDKImpl.hash(signedCredential).value.toArray)
      val credentialData = CredentialData(issuer = issuerDID.stripPrismPrefix, contentHash = contentHash)
      val issueCredentialOperation = IssueCredentialOperation(Some(credentialData))
      (
        AtalaOperation(AtalaOperation.Operation.IssueCredential(issueCredentialOperation)),
        signedCredential.canonicalForm
      )
    }
  }

  def signedAtalaOperation(ecKeyPair: ECKeyPair, func: => AtalaOperation): SignedAtalaOperation = {
    val atalaOperation = func
    val signature = EC.sign(atalaOperation.toByteArray, ecKeyPair.privateKey)
    SignedAtalaOperation(
      signedWith = firstMasterKeyId,
      signature = ByteString.copyFrom(signature.data),
      operation = Some(atalaOperation)
    )
  }

  private def toECKeyData(publicKey: ECPublicKey): ECKeyData = {
    val point = publicKey.getCurvePoint

    ECKeyData()
      .withCurve(CURVE_NAME)
      .withX(ByteString.copyFrom(point.x.toByteArray))
      .withY(ByteString.copyFrom(point.y.toByteArray))
  }

  private def toPublicKey(
      id: String,
      ecKeyData: ECKeyData,
      keyUsage: KeyUsage
  ) = {
    PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)
  }

  // TODO: Move these two methods to EC.
  def ecKeyPairFromSeed(mnemonic: Mnemonic): ECKeyPair = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    toKeyPair(root.derivePath(firstMasterChild))
  }

  private def toKeyPair(root: BIP32Interface): ECKeyPair = {
    val privateKey = BigIntOps.toBigInt(toBytes(root.privateKey.get))
    EC.toKeyPairFromPrivateKey(privateKey)
  }

  private def toBytes(buffer: Buffer): Array[Byte] = {
    val len = buffer.length.toInt
    val bytes = new Array[Byte](len)
    for (i <- 0 until len) {
      bytes(i) = buffer(i).get.toByte
    }
    bytes
  }

}
