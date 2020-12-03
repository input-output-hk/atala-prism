package io.iohk.atala.prism.node.poc.batch

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.{
  BatchData,
  CredentialBatchId,
  CredentialContent,
  CredentialVerification,
  KeyData,
  VerificationError
}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPrivateKey, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.protos.{node_api, node_models}
import org.scalatest.OptionValues._
import io.iohk.atala.prism.credentials.json.implicits._
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api.GetCredentialRevocationTimeRequest

case class PrismWallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  implicit val ec = EC

  private val masterKeyPair = EC.generateKeyPair()
  private val masterPrivateKey = masterKeyPair.privateKey
  private val masterPublicKey = masterKeyPair.publicKey
  private val issuanceKeyPair = EC.generateKeyPair()
  private val issuancePrivateKey = issuanceKeyPair.privateKey
  private val issuancePublicKey = issuanceKeyPair.publicKey

  private var dids: Map[DID, Map[String, ECPrivateKey]] = Map()

  def generateDID(): (DID, node_models.AtalaOperation) = {
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
    val did = DID.buildPrismDID(operationHash.hexValue)

    dids += (did -> Map(
      "master0" -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (did, atalaOp)
  }

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      did: DID
  ): node_models.SignedAtalaOperation = {
    val key = dids(did)(keyId)
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(EC.sign(operation.toByteArray, key).data)
    )
  }

  def signCredential(
      credential: String,
      keyId: String,
      signerDID: DID
  ): JsonBasedCredential[CredentialContent[String]] = {
    val privateKey = dids(signerDID)(keyId)
    JsonBasedCredential
      .fromCredentialContent[CredentialContent[String]](
        CredentialContent[String](
          credentialType = Seq(),
          issuerDid = Some(signerDID),
          issuanceKeyId = Some(keyId),
          issuanceDate = None,
          expiryDate = None,
          credentialSubject = Some(credential)
        )
      )
      .sign(privateKey)
  }

  def verifyCredential[C](
      credential: JsonBasedCredential[C],
      merkleRoot: MerkleRoot,
      merkleProof: MerkleInclusionProof
  ): ValidatedNel[VerificationError, Unit] = {
    // extract user DIDSuffix and keyId from credential
    val jsonContent =
      credential.json
        .flatMap(_.as[CredentialContent[Nothing]])
        .getOrElse(throw new RuntimeException("invalid json"))
    val issuerDID = jsonContent.issuerDid.value
    val issuanceKeyId = jsonContent.issuanceKeyId.value

    // request credential state to the node
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
      GetCredentialRevocationTimeRequest()
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
