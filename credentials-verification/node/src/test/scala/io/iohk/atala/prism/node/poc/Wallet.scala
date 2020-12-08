package io.iohk.atala.prism.node.poc

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.{
  Credential,
  CredentialBatchId,
  BatchData,
  KeyData,
  CredentialVerification,
  VerificationError
}
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPrivateKey, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.poc.NodeSDK
import io.iohk.atala.prism.protos.{node_api, node_models}
import org.scalatest.OptionValues._
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof

// We define some classes to illustrate what happens in the different components
case class Wallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  implicit val ec = EC

  val masterKeyPair = EC.generateKeyPair()
  val masterPrivateKey = masterKeyPair.privateKey
  val masterPublicKey = masterKeyPair.publicKey
  val issuanceKeyPair = EC.generateKeyPair()
  val issuancePrivateKey = issuanceKeyPair.privateKey
  val issuancePublicKey = issuanceKeyPair.publicKey

  private var dids: Map[DIDSuffix, Map[String, ECPrivateKey]] = Map()

  def generateDID(): (DIDSuffix, node_models.AtalaOperation) = {
    // This could be encapsulated in the "NodeSDK". I added it here for simplicity
    // Note that in our current design we cannot create a did that has two keys from start
    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = "master0",
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
    val didSuffix = DIDSuffix.unsafeFromDigest(operationHash)

    dids += (didSuffix -> Map(
      "master0" -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (didSuffix, atalaOp)
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
      signature = ByteString.copyFrom(EC.sign(operation.toByteArray, key).data)
    )
  }

  def signCredential(
      credentialContent: CredentialContent,
      keyId: String,
      didSuffix: DIDSuffix
  ): Credential = {
    val privateKey = dids(didSuffix)(keyId)
    Credential.fromCredentialContent(credentialContent).sign(privateKey)
  }

  def verifyCredential(credential: Credential): Boolean = {
    // extract user DIDSuffix and keyId from credential
    val issuerDID = GenericCredentialsSDK.getIssuerDID(credential.canonicalForm)
    val issuerDIDSuffix = GenericCredentialsSDK.getIssuerDIDSufix(credential.canonicalForm)
    val issuanceKeyId = GenericCredentialsSDK.getKeyId(credential.canonicalForm)

    // request credential state to the node
    val issuanceOperation = NodeSDK.buildIssueCredentialOp(credential.hash, issuerDIDSuffix)
    val credentialId = NodeSDK.computeCredId(issuanceOperation)

    val credentialState = node.getCredentialState(
      node_api.GetCredentialStateRequest(
        credentialId.id
      )
    )
    val credentialIssuanceDate = ProtoCodecs.fromTimestampInfoProto(credentialState.publicationDate.value)
    val credentialRevocationDate = credentialState.revocationDate map ProtoCodecs.fromTimestampInfoProto

    println(credentialState)

    // resolve DID through the node
    val didDocumentOption = node
      .getDidDocument(
        node_api.GetDidDocumentRequest(
          did = issuerDID
        )
      )
      .document
    val didDocument = didDocumentOption.value

    // get verification key
    val issuancekeyProtoOption = didDocument.publicKeys.find(_.id == issuanceKeyId)
    val issuancekeyData = issuancekeyProtoOption.value
    val issuanceKeyAddedOn = ProtoCodecs.fromTimestampInfoProto(issuancekeyData.addedOn.value)
    val issuanceKeyRevokedOn = issuancekeyData.revokedOn map ProtoCodecs.fromTimestampInfoProto
    val issuancekey = issuancekeyProtoOption flatMap ProtoCodecs.fromProtoKey

    // run all verifications, including signature

    // the credential was posted in the chain, and
    credentialState.publicationDate.nonEmpty &&
    credentialRevocationDate.isEmpty &&
    // the issuer DID that signed the credential is registered, and
    didDocumentOption.nonEmpty &&
    // the key used to signed the credential is in the DID, and
    issuancekeyProtoOption.nonEmpty &&
    // the key was in the DID before the credential publication event, and
    issuanceKeyAddedOn.occurredBefore(credentialIssuanceDate) &&
    // the key was not revoked before credential publication event, and
    (
      // either the key was never revoked
      issuanceKeyRevokedOn.isEmpty ||
      // or was revoked after signing the credential
      credentialIssuanceDate.occurredBefore(issuanceKeyRevokedOn.value)
    ) &&
    // the signature is valid
    credential.isValidSignature(issuancekey.value)
  }

  def verifyCredential(
      credential: Credential,
      merkleProof: MerkleInclusionProof
  ): ValidatedNel[VerificationError, Unit] = {
    // extract user DIDSuffix and keyId from credential
    val issuerDID = credential.content.issuerDid match {
      case Left(error) => throw error
      case Right(value) => value
    }
    val issuanceKeyId = credential.content.issuanceKeyId match {
      case Left(error) => throw error
      case Right(value) => value
    }

    // request credential state to the node
    val merkleRoot = merkleProof.derivedRoot
    val batchId = CredentialBatchId.fromBatchData(issuerDID.suffix, merkleRoot)

    val batchStateProto = node.getBatchState(
      node_api.GetBatchStateRequest(
        batchId.id
      )
    )
    val batchIssuanceDate = ProtoCodecs.fromTimestampInfoProto(batchStateProto.publicationDate.value)
    val batchRevocationDate = batchStateProto.revocationDate.map(ProtoCodecs.fromTimestampInfoProto)
    val batchData = BatchData(batchIssuanceDate, batchRevocationDate)

    // resolve DID through the node
    val didDocumentOption = node
      .getDidDocument(
        node_api.GetDidDocumentRequest(
          did = issuerDID.value
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

    val keyData = KeyData(issuanceKey, issuanceKeyAddedOn, issuanceKeyRevokedOn)

    // request specific credential revocation status to the node
    val credentialHash = credential.hash
    val credentialRevocationTimeResponse = node.getCredentialRevocationTime(
      node_api
        .GetCredentialRevocationTimeRequest()
        .withBatchId(batchId.id)
        .withCredentialHash(ByteString.copyFrom(credentialHash.value.toArray))
    )
    val credentialRevocationTime =
      credentialRevocationTimeResponse.revocationDate
        .map(ProtoCodecs.fromTimestampInfoProto)

    CredentialVerification
      .verifyCredential(
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
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }
}
