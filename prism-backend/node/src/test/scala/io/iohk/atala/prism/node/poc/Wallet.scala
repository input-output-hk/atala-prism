package io.iohk.atala.prism.node.poc

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.crypto.{MerkleInclusionProof, Sha256}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.models.{DidSuffix, KeyData}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.poc.CredVerification.{BatchData, VerificationError}
import org.scalatest.OptionValues.convertOptionToValuable

// We define some classes to illustrate what happens in the different components
case class Wallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {

  private var dids: Map[DidSuffix, collection.mutable.Map[String, ECPrivateKey]] = Map()

  def generateDID(): (DidSuffix, node_models.AtalaOperation) = {
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
        node_models.CreateDIDOperation.DIDCreationData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = PrismDid.getDEFAULT_MASTER_KEY_ID,
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
    val operationHash = Sha256.compute(atalaOp.toByteArray)
    val didSuffix: DidSuffix = DidSuffix(operationHash.getHexValue)

    dids += (didSuffix -> collection.mutable.Map(
      PrismDid.getDEFAULT_MASTER_KEY_ID -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (didSuffix, atalaOp)
  }

  def addRevocationKeyToDid(
      revocationKeyId: String,
      previousOperationHash: ByteString,
      didSuffix: DidSuffix
  ): Unit = {
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
      id = didSuffix.getValue,
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
      PrismDid.getDEFAULT_MASTER_KEY_ID,
      didSuffix
    )
    node.scheduleOperations(node_api.ScheduleOperationsRequest(List(updateDidOpSigned)))
    dids(didSuffix) += (revocationKeyId -> revocationKeyPair.getPrivateKey)
    ()
  }

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      didSuffix: DidSuffix
  ): node_models.SignedAtalaOperation = {
    // TODO: This logic should also live eventually in the crypto library
    val key = dids(didSuffix)(keyId)
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(EC.signBytes(operation.toByteArray, key).getData)
    )
  }

  def signCredential(
      credentialContent: CredentialContent,
      keyId: String,
      didSuffix: DidSuffix
  ): PrismCredential = {
    val privateKey = dids(didSuffix)(keyId)
    new JsonBasedCredential(credentialContent, null).sign(privateKey)
  }

  def signKey(
      publicKey: ECPublicKey,
      keyId: String,
      didSuffix: DidSuffix
  ): ECSignature = {
    val privateKey = dids(didSuffix)(keyId)
    EC.signBytes(publicKey.getEncoded, privateKey)
  }

  def verifySignedKey(
      publicKey: ECPublicKey,
      signature: ECSignature,
      signingKey: ECPublicKey
  ): Boolean = {
    EC.verifyBytes(publicKey.getEncoded, signingKey, signature)
  }

  def verifyCredential(
      credential: PrismCredential,
      merkleProof: MerkleInclusionProof
  ): ValidatedNel[VerificationError, Unit] = {
    // extract user DIDSuffix and keyId from credential
    val issuerDID = Option(credential.getContent.getIssuerDid)
      .getOrElse(throw new Exception("getIssuerDid is null"))
    val issuanceKeyId =
      Option(credential.getContent.getIssuanceKeyId)
        .getOrElse(throw new Exception("getIssuanceKeyId is null"))

    // request credential state to the node
    val merkleRoot = merkleProof.derivedRoot
    val batchId =
      CredentialBatchId.fromBatchData(issuerDID.getSuffix, merkleRoot)

    val batchStateProto = node.getBatchState(
      node_api.GetBatchStateRequest(
        batchId.getId
      )
    )
    val batchIssuanceDate =
      ProtoCodecs.fromTimestampInfoProto(
        batchStateProto.getPublicationLedgerData.timestampInfo.value
      )
    val batchRevocationDate =
      batchStateProto.getRevocationLedgerData.timestampInfo.map(
        ProtoCodecs.fromTimestampInfoProto
      )
    val batchData = BatchData(batchIssuanceDate, batchRevocationDate)

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
    val issuancekeyProtoOption =
      didDocument.publicKeys.find(_.id == issuanceKeyId)
    val issuancekeyData = issuancekeyProtoOption.value
    val issuanceKey =
      issuancekeyProtoOption.flatMap(ProtoCodecs.fromProtoKey).value
    val issuanceKeyAddedOn = ProtoCodecs.fromTimestampInfoProto(
      issuancekeyData.addedOn.value.timestampInfo.value
    )
    val issuanceKeyRevokedOn =
      issuancekeyData.revokedOn.flatMap(
        _.timestampInfo.map(ProtoCodecs.fromTimestampInfoProto)
      )

    val keyData = KeyData(issuanceKey, issuanceKeyAddedOn, issuanceKeyRevokedOn)

    // request specific credential revocation status to the node
    val credentialHash = credential.hash
    val credentialRevocationTimeResponse = node.getCredentialRevocationTime(
      node_api
        .GetCredentialRevocationTimeRequest()
        .withBatchId(batchId.getId)
        .withCredentialHash(ByteString.copyFrom(credentialHash.getValue))
    )
    val credentialRevocationTime =
      credentialRevocationTimeResponse.getRevocationLedgerData.timestampInfo
        .map(ProtoCodecs.fromTimestampInfoProto)

    CredVerification
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
