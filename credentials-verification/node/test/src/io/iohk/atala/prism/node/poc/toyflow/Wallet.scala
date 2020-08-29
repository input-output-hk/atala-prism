package io.iohk.atala.prism.node.poc.toyflow

import java.nio.charset.StandardCharsets
import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.poc.{CryptoSDKImpl, SignedCredential}
import io.iohk.atala.prism.crypto.{ECKeys, ECSignature, SHA256Digest}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.poc.NodeSDK
import io.iohk.prism.protos.{node_api, node_models}
import org.scalatest.OptionValues._

// We define some classes to illustrate what happens in the different components
case class Wallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {
  private val cryptoSDK = CryptoSDKImpl

  val masterKeyPair = ECKeys.generateKeyPair()
  val masterPrivateKey = masterKeyPair.getPrivate
  val masterPublicKey = masterKeyPair.getPublic
  val issuanceKeyPair = ECKeys.generateKeyPair()
  val issuancePrivateKey = issuanceKeyPair.getPrivate
  val issuancePublicKey = issuanceKeyPair.getPublic

  private var dids: Map[DIDSuffix, Map[String, PrivateKey]] = Map()

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
    val didSuffix = DIDSuffix(operationHash)

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
      signature = ByteString.copyFrom(ECSignature.sign(key, operation.toByteArray).toArray)
    )
  }

  def signCredential(
      credential: String,
      keyId: String,
      didSuffix: DIDSuffix
  ): SignedCredential = {
    val privateKey = dids(didSuffix)(keyId)
    val credentialBytes = credential.getBytes(StandardCharsets.UTF_8)
    cryptoSDK.signCredential(privateKey, credentialBytes)
  }

  def verifyCredential(signedCredential: SignedCredential): Boolean = {
    // get credential hash and compute id
    val (cred, _) = SignedCredential.decode(signedCredential)
    val concreteRepresentation = new String(cred, StandardCharsets.UTF_8)

    // extract user DIDSuffix and keyId from credential
    val issuerDID = GenericCredentialsSDK.getIssuerDID(concreteRepresentation)
    val issuerDIDSuffix = GenericCredentialsSDK.getIssuerDIDSufix(concreteRepresentation)
    val issuanceKeyId = GenericCredentialsSDK.getKeyId(concreteRepresentation)

    // request credential state to the node
    val hash = cryptoSDK.hash(signedCredential)
    val issuanceOperation = NodeSDK.buildIssueCredentialOp(hash, issuerDIDSuffix)
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
    val issuancekey = issuancekeyProtoOption flatMap ProtoCodecs.fromProtoKeyLegacy

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
    cryptoSDK.verify(issuancekey.value, signedCredential)
  }

  private def publicKeyToProto(key: PublicKey): node_models.ECKeyData = {
    val point = ECKeys.getECPoint(key)
    node_models.ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }
}
