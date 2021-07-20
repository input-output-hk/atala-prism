package io.iohk.atala.cvp.webextension.common

import com.google.protobuf.ByteString
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.prism.protos.node_models._
import typings.bip32.bip32Mod.BIP32Interface
import typings.bip32.{mod => bip32}
import typings.inputOutputHkPrismSdk.mod.Nullable
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.content.CredentialContentCompanion
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.{CredentialBatches, PrismCredential}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.keys.{ECKeyPair, ECPublicKey}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.{EC, MerkleInclusionProof}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.extras.{toArray, toList => toKotlingList}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.DIDCompanion.masterKeyId
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.{DID, DIDCompanion, DIDSuffix}

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.{Int8Array, byteArray2Int8Array, int8Array2ByteArray}
import scala.util.control.NonFatal

object ECKeyOperation {
  val CURVE_NAME = "secp256k1"

  // https://github.com/input-output-hk/atala/blob/develop/credentials-verification/docs/protocol/key-derivation.md
  private val firstMasterChild = "m/0'/0'/0'"
  private val firstIssuingChild = "m/0'/1'/0'"

  // TODO: this key id should eventually be selected by the user
  // which should be done when we complete the key derivation flow.
  val issuingKeyId = "issuing0"

  def unpublishedDidFromMnemonic(mnemonic: Mnemonic): DID = {
    val masterECKeyPair = ECKeyOperation.masterECKeyPairFromSeed(mnemonic)
    val issuingECKeyPair = ECKeyOperation.issuingECKeyPairFromSeed(mnemonic)
    DIDCompanion.createUnpublishedDID(masterECKeyPair.publicKey, issuingECKeyPair.publicKey)
  }

  def createDIDAtalaOperation(mnemonic: Mnemonic): AtalaOperation = {
    val masterECKeyPair = ECKeyOperation.masterECKeyPairFromSeed(mnemonic)
    val issuingECKeyPair = ECKeyOperation.issuingECKeyPairFromSeed(mnemonic)
    val masterPublicKeyProto =
      toPublicKey(masterKeyId, toECKeyData(masterECKeyPair.publicKey), KeyUsage.MASTER_KEY)
    val issuingPublicKeyProto =
      toPublicKey(issuingKeyId, toECKeyData(issuingECKeyPair.publicKey), KeyUsage.ISSUING_KEY)
    val didData = DIDData(publicKeys = Seq(masterPublicKeyProto, issuingPublicKeyProto))
    val createDIDOperation = CreateDIDOperation(Some(didData))
    val atalaOperation = AtalaOperation(AtalaOperation.Operation.CreateDid(createDIDOperation))
    atalaOperation
  }

  def issuerOperation(
      issuerDID: DID,
      signingKeyId: String,
      signingKey: ECKeyPair,
      credentialsData: List[CredentialData]
  ): (AtalaOperation, List[(String, MerkleInclusionProof)]) = {
    // The long-form DID may be used in the wallet, but only the canonical one can issue credentials.
    val canonicalIssuerDID =
      try {
        val didSuffix = issuerDID.getCanonicalSuffix().get.asInstanceOf[DIDSuffix]
        DIDCompanion.buildPrismDIDFromSuffix(didSuffix)
      } catch {
        case NonFatal(_) =>
          throw new RuntimeException("There is no way to get the canonical DID which is required to issue credentials")
      }

    val signedCredentials: List[PrismCredential] = credentialsData.map { cd =>
      val content = CredentialContentCompanion
        .fromString(
          s"""{
           |  "issuerDid": "${canonicalIssuerDID.value}",
           |  "keyId": "$signingKeyId",
           |  "credentialSubject": ${cd.credentialClaims}
           |}""".stripMargin
        )

      new JsonBasedCredential(content, null.asInstanceOf[Nullable[ECSignature]])
        .sign(signingKey.privateKey)
    }

    val batchResult = CredentialBatches.batch(toKotlingList(signedCredentials.toJSArray))
    val merkleRoot = batchResult.root
    val proofs = toArray[MerkleInclusionProof](batchResult.proofs)
    val merkleRootProto = ByteString.copyFrom(int8Array2ByteArray(merkleRoot.hash.value))
    // This requires the suffix only, as the node stores only suffixes
    val credentialBatchData =
      CredentialBatchData(issuerDid = canonicalIssuerDID.suffix.value, merkleRoot = merkleRootProto)
    val issueCredentialOperation = IssueCredentialBatchOperation(Some(credentialBatchData))
    val credentialsAndProofs =
      signedCredentials.map(_.canonicalForm).zip(proofs)

    println(s"${signedCredentials.size} credentials signed")
    println(s"proof = '${proofs.headOption.map(_.encode()).getOrElse("")}'")
    println(s"credential = '${signedCredentials.map(_.canonicalForm).headOption.getOrElse("")}'")

    (
      AtalaOperation(AtalaOperation.Operation.IssueCredentialBatch(issueCredentialOperation)),
      credentialsAndProofs
    )
  }

  def signedAtalaOperation(keyId: String, ecKeyPair: ECKeyPair, func: => AtalaOperation): SignedAtalaOperation = {
    val atalaOperation = func

    val signature = EC.signBytes(byteArray2Int8Array(atalaOperation.toByteArray), ecKeyPair.privateKey)
    SignedAtalaOperation(
      signedWith = keyId,
      signature = ByteString.copyFrom(int8Array2ByteArray(signature.getEncoded())),
      operation = Some(atalaOperation)
    )
  }

  private def toECKeyData(publicKey: ECPublicKey): ECKeyData = {
    val point = publicKey.getCurvePoint()

    ECKeyData()
      .withCurve(CURVE_NAME)
      .withX(ByteString.copyFrom(int8Array2ByteArray(point.xBytes())))
      .withY(ByteString.copyFrom(int8Array2ByteArray(point.yBytes())))
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
  def masterECKeyPairFromSeed(mnemonic: Mnemonic): ECKeyPair = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    toKeyPair(root.derivePath(firstMasterChild))
  }

  def issuingECKeyPairFromSeed(mnemonic: Mnemonic): ECKeyPair = {
    val root: BIP32Interface = bip32.fromSeed(mnemonic.toSyncBuffer)
    toKeyPair(root.derivePath(firstIssuingChild))
  }

  private def toKeyPair(root: BIP32Interface): ECKeyPair = {
    val privateKeyByteArray = new Int8Array(root.privateKey.get.buffer)
    val privateKey = EC.toPrivateKeyFromBytes(privateKeyByteArray)
    val publicKey = EC.toPublicKeyFromPrivateKey(privateKey)
    new ECKeyPair(publicKey, privateKey)
  }

}
