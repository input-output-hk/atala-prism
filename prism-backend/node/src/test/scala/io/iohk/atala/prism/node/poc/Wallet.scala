package io.iohk.atala.prism.node.poc

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import com.google.protobuf.ByteString
import io.iohk.atala.prism.interop.CredentialContentConverter._
import io.iohk.atala.prism.kotlin.credentials._
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.kotlin.crypto.keys.{ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, Sha256Digest}
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.Sha256
import io.iohk.atala.prism.kotlin.extras.{
  CredentialVerificationService,
  CredentialVerificationServiceAsync,
  ProtoClientUtils,
  VerificationError
}
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.protos.{node_api, node_models}
import kotlinx.coroutines.{BuildersKt, CoroutineStart, Deferred, GlobalScope}

import scala.util.Try

// We define some classes to illustrate what happens in the different components
case class Wallet(node: node_api.NodeServiceGrpc.NodeServiceBlockingStub) {

  private var dids: Map[String, collection.mutable.Map[String, ECPrivateKey]] = Map()

  def generateDID(): (String, node_models.AtalaOperation) = {
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
              id = PrismDid.getMASTER_KEY_ID,
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
    val didSuffix: String = operationHash.getHexValue()

    dids += (didSuffix -> collection.mutable.Map(
      PrismDid.getMASTER_KEY_ID -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (didSuffix, atalaOp)
  }

  def addRevocationKeyToDid(revocationKeyId: String, previousOperationHash: ByteString, didSuffix: String): Unit = {
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
      id = didSuffix,
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
      PrismDid.getMASTER_KEY_ID,
      didSuffix
    )
    node.updateDID(node_api.UpdateDIDRequest(Some(updateDidOpSigned)))
    dids(didSuffix) += (revocationKeyId -> revocationKeyPair.getPrivateKey)
    ()
  }

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      didSuffix: String
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
      didSuffix: String
  ): PrismCredential = {
    val privateKey = dids(didSuffix)(keyId)
    val credentialString = credentialContent.asString
    JsonBasedCredential.fromString(credentialString).sign(privateKey)
  }

  def signKey(
      publicKey: ECPublicKey,
      keyId: String,
      didSuffix: String
  ): ECSignature = {
    val privateKey = dids(didSuffix)(keyId)
    EC.signBytes(publicKey.getEncoded, privateKey)
  }

  def verifySignedKey(publicKey: ECPublicKey, signature: ECSignature, signingKey: ECPublicKey): Boolean = {
    EC.verifyBytes(publicKey.getEncoded, signingKey, signature)
  }

  def verifyCredential(
      credential: PrismCredential,
      merkleProof: MerkleInclusionProof
  ): ValidatedNel[VerificationError, Unit] = {

    val nodeKotlin = ProtoClientUtils.INSTANCE.nodeClient("localhost", 50053)
    val credentialVerificationService = new CredentialVerificationService(nodeKotlin)
    val credentialVerificationServiceAsync = new CredentialVerificationServiceAsync(credentialVerificationService)
    val result = credentialVerificationServiceAsync.verify(credential, merkleProof)
    val errors = result.join().getVerificationErrors

    import scala.jdk.CollectionConverters._
    Validated.fromEither {

      Either.cond(errors.isEmpty, (), NonEmptyList.fromListUnsafe(errors.asScala.toList))
    }
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
