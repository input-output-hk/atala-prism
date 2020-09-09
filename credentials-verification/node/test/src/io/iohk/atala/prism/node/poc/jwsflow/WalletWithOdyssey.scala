package io.iohk.atala.prism.node.poc.jwsflow

import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{ECKeys, ECSignature, SHA256Digest}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.DIDSuffix
import io.iohk.atala.prism.node.poc.NodeSDK
import io.iohk.atala.prism.protos.{node_api, node_models}
import net.jtownson.odyssey.VC.VCField.{CredentialSubjectField, EmptyField, IssuanceDateField, IssuerField}
import net.jtownson.odyssey.Verifier.Es256Verifier
import net.jtownson.odyssey.{Jws, PublicKeyResolver, VC, VCDataModel}
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._

case class WalletWithOdyssey(
    node: node_api.NodeServiceGrpc.NodeServiceBlockingStub,
    keyResolver: PublicKeyResolver
) {

  // there should probably be
  private def getKeyId(jws: String): String = Jws.parse(jws).get.protectedHeaders("kid").noSpaces

  private val masterKeyPair = ECKeys.generateKeyPair()
  private val masterPrivateKey = masterKeyPair.getPrivate
  private val masterPublicKey = masterKeyPair.getPublic
  private val issuanceKeyPair = ECKeys.generateKeyPair()
  private val issuancePrivateKey = issuanceKeyPair.getPrivate
  private val issuancePublicKey = issuanceKeyPair.getPublic

  private var dids: Map[String, Map[String, PrivateKey]] = Map()

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

    dids += (s"did:prism:$didSuffix" -> Map(
      "master0" -> masterPrivateKey,
      "issuance0" -> issuancePrivateKey
    ))

    (didSuffix, atalaOp)
  }

  def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      did: String
  ): node_models.SignedAtalaOperation = {
    // TODO: This logic should also live eventually in the crypto library
    val key = dids(did)(keyId)
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(ECSignature.sign(key, operation.toByteArray).toArray)
    )
  }

  def signCredential(
      credential: VC[EmptyField with IssuerField with IssuanceDateField with CredentialSubjectField],
      keyId: String,
      did: String
  ): Jws[Jws.JwsField.EmptyField with Jws.JwsField.SignatureField] = {
    val privateKey = dids(did)(keyId)
    credential
      .withEs256Signature(new URI(s"$did#$keyId"), privateKey)
      .toJws
  }

  def verifyCredential(compactJws: String): Boolean = {
    // get credential hash and compute id
    val verifier = new Es256Verifier(keyResolver)

    import scala.concurrent.ExecutionContext.Implicits.global

    // TODO: I would suggest to separate parsing/model translation from the fact of
    //       verifying the signature
    val parsedVCDataModel = VCDataModel.fromJwsCompactSer(verifier, compactJws).futureValue

    // extract user DIDSuffix and keyId from credential
    val issuerDID = parsedVCDataModel.issuer.noSpaces.drop(1).dropRight(1)
    // we remove the external quotes (") the part "did:prism:64 chars suffix" and the separator "#"
    val issuanceKeyId = getKeyId(compactJws).drop(66 + "did:prism:".length).dropRight(1)

    // request credential state to the node
    val hash = SHA256Digest.compute(compactJws.getBytes(StandardCharsets.UTF_8))
    val issuerDIDSuffix = DIDSuffix(issuerDID.drop("did:prism:".length))
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

    val issuancekeyProtoOption = didDocument.publicKeys.find(_.id == issuanceKeyId)
    val issuancekeyData = issuancekeyProtoOption.value
    val issuanceKeyAddedOn = ProtoCodecs.fromTimestampInfoProto(issuancekeyData.addedOn.value)
    val issuanceKeyRevokedOn = issuancekeyData.revokedOn map ProtoCodecs.fromTimestampInfoProto

    // run all verifications, inclusing signature

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
    )
    // the signature is valid
    // NOTE: Signature validation is done when the JWS is decoded to VCDataModel
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
