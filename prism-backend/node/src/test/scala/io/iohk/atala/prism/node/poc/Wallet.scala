package io.iohk.atala.prism.node.poc

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.circe.syntax.EncoderOps
import io.iohk.atala.prism.kotlin.credentials.{
  BatchData,
  CredentialBatchId,
  CredentialVerification,
  KeyData,
  PrismCredential,
  VerificationError
}
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.{ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.protos.{node_api, node_models}
import org.scalatest.OptionValues._
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.identity.DID.masterKeyId
import io.iohk.atala.prism.interop.toScalaSDK._
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.interop.toKotlinSDK._
import io.iohk.atala.prism.interop.CredentialContentConverter._

// We define some classes to illustrate what happens in the different components
case class Wallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  implicit val ec = EC

  private var dids: Map[DIDSuffix, collection.mutable.Map[String, ECPrivateKey]] = Map()

  def generateDID(): (DIDSuffix, node_models.AtalaOperation) = {
    val masterKeyPair = EC.generateKeyPair()
    val masterPrivateKey = masterKeyPair.getPrivateKey
    val masterPublicKey = masterKeyPair.getPublicKey
    val issuanceKeyPair = EC.generateKeyPair()
    val issuancePrivateKey = issuanceKeyPair.getPrivateKey
    val issuancePublicKey = issuanceKeyPair.getPublicKey

    // This could be encapsulated in the "NodeSDK". I added it here for simplicity
    // Note that in our current design we cannot create a did that has two keys from start
    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = masterKeyId,
              usage = node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                publicKeyToProto(masterPublicKey)
              )
            ),
            node_models.PublicKey(
              id = "issuance0",
              usage = node_models.KeyUsage.ISSUING_KEY,
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                publicKeyToProto(issuancePublicKey)
              )
            )
          )
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)
    val didSuffix: DIDSuffix = DIDSuffix.Companion.fromDigest(operationHash)

    dids += (didSuffix -> collection.mutable.Map(
      masterKeyId -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (didSuffix, atalaOp)
  }

  def addRevocationKeyToDid(revocationKeyId: String, previousOperationHash: ByteString, didSuffix: DIDSuffix): Unit = {
    val revocationKeyPair = EC.generateKeyPair()
    val publicKeyProto = node_models.PublicKey(
      id = revocationKeyId,
      usage = node_models.KeyUsage.REVOCATION_KEY,
      keyData = node_models.PublicKey.KeyData.EcKeyData(
        publicKeyToProto(revocationKeyPair.getPublicKey)
      )
    )

    val updateDIDOp = node_models.UpdateDIDOperation(
      previousOperationHash = previousOperationHash,
      id = didSuffix.value,
      actions = Seq(
        node_models.UpdateDIDAction(
          node_models.UpdateDIDAction.Action.AddKey(
            node_models.AddKeyAction(
              Some(publicKeyProto)
            )
          )
        )
      )
    )
    val updateDidOpSigned = signOperation(
      node_models.AtalaOperation(
        node_models.AtalaOperation.Operation.UpdateDid(updateDIDOp)
      ),
      masterKeyId,
      didSuffix
    )
    node.updateDID(node_api.UpdateDIDRequest(Some(updateDidOpSigned)))
    dids(didSuffix) += (revocationKeyId -> revocationKeyPair.getPrivateKey)
    ()
  }

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      didSuffix: DIDSuffix
  ): node_models.SignedAtalaOperation = {
    // TODO: This logic should also live eventually in the crypto library
    val key = dids(didSuffix)(keyId)
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(EC.sign(operation.toByteArray, key).getData)
    )
  }

  def signCredential(
      credentialContent: CredentialContent,
      keyId: String,
      didSuffix: DIDSuffix
  ): PrismCredential = {
    val privateKey = dids(didSuffix)(keyId)
    val credentialString = credentialContent.asString
    kotlinx.serialization.json.JsonKt.Json(null, null).encodeToString(null, credentialContent)
    JsonBasedCredential.fromString(credentialString).sign(privateKey.asKotlin)
  }

  def signKey(
      publicKey: ECPublicKey,
      keyId: String,
      didSuffix: DIDSuffix
  ): ECSignature = {
    val privateKey = dids(didSuffix)(keyId)
    EC.sign(publicKey.getEncoded, privateKey)
  }

  def verifySignedKey(publicKey: ECPublicKey, signature: ECSignature, signingKey: ECPublicKey): Boolean = {
    EC.verify(publicKey.getEncoded, signingKey, signature)
  }

  def verifyCredential(
      credential: PrismCredential,
      merkleProof: MerkleInclusionProof
  ): ValidatedNel[VerificationError, Unit] = {
    // extract user DIDSuffix and keyId from credential
    val issuerDID = Option(credential.getContent.getIssuerDid) match {
      case None => throw new Exception("getIssuerDid is null")
      case Some(value) => value
    }
    val issuanceKeyId = Option(credential.getContent.getIssuanceKeyId) match {
      case None => throw new Exception("getIssuanceKeyId is null")
      case Some(value) => value
    }

    // request credential state to the node
    val merkleRoot = merkleProof.derivedRoot
    val batchId = CredentialBatchId.fromBatchData(issuerDID.getSuffix, merkleRoot.asKotlin)

    val batchStateProto = node.getBatchState(
      node_api.GetBatchStateRequest(
        batchId.getId
      )
    )
    val batchIssuanceDate =
      ProtoCodecs.fromTimestampInfoProto(batchStateProto.getPublicationLedgerData.timestampInfo.value)
    val batchRevocationDate =
      batchStateProto.getRevocationLedgerData.timestampInfo.map(ProtoCodecs.fromTimestampInfoProto)
    val batchData = new BatchData(batchIssuanceDate, batchRevocationDate.orNull)

    // resolve DID through the node
    val didDocumentOption = node
      .getDidDocument(
        node_api.GetDidDocumentRequest(
          did = issuerDID.getValue
        )
      )
      .document
    val didDocument = didDocumentOption.value

    // get verification key
    val issuancekeyProtoOption = didDocument.publicKeys.find(_.id == issuanceKeyId)
    val issuancekeyData = issuancekeyProtoOption.value
    val issuanceKey = issuancekeyProtoOption.flatMap(ProtoCodecs.fromProtoKey).value
    val issuanceKeyAddedOn = ProtoCodecs.fromTimestampInfoProto(issuancekeyData.addedOn.value)
    val issuanceKeyRevokedOn = issuancekeyData.revokedOn.map(ProtoCodecs.fromTimestampInfoProto)

    val keyData = new KeyData(issuanceKey.asKotlin, issuanceKeyAddedOn, issuanceKeyRevokedOn.orNull)

    // request specific credential revocation status to the node
    val credentialHash = credential.hash
    val credentialRevocationTimeResponse = node.getCredentialRevocationTime(
      node_api
        .GetCredentialRevocationTimeRequest()
        .withBatchId(batchId.getId)
        .withCredentialHash(ByteString.copyFrom(credentialHash.getValue.toArray))
    )
    val credentialRevocationTime =
      credentialRevocationTimeResponse.getRevocationLedgerData.timestampInfo
        .map(ProtoCodecs.fromTimestampInfoProto)

    PrismCredentialVerification
      .verify(
        keyData,
        batchData,
        credentialRevocationTime,
        merkleRoot,
        merkleProof,
        credential
      )
  }

  private def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }
}
