package io.iohk.atala.prism.node.poc.endorsements

import java.time.Instant
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, CredentialBatches}
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.protos.endorsements_api._
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{KeyUsage, SignedAtalaOperation}
import io.iohk.atala.prism.utils.syntax.InstantToTimestampOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class EndorsementsService(
    nodeServiceStub: NodeServiceGrpc.NodeServiceBlockingStub
)(implicit
    executionContext: ExecutionContext
) extends EndorsementsServiceGrpc.EndorsementsService {
  import EndorsementsService._

  // private state
  private var moePrismDid: PrismDid = _
  private var trustedPrismDids: Set[PrismDid] = Set.empty
  private var signedKeys: List[SignedKey] = List.empty
  private var requestedBy: Map[ECPublicKey, PrismDid] = Map.empty
  private var endorsedBy: Map[PrismDid, PrismDid] = Map.empty
  private var keyAssigned: Map[PrismDid, ECPublicKey] = Map.empty
  private var validIn: Map[PrismDid, List[ValidInterval]] = Map.empty.withDefaultValue(Nil)

  private var lastRequested: Int = -1
  private def nextKey(): SignedKey = {
    lastRequested += 1
    if (lastRequested < signedKeys.size) signedKeys(lastRequested)
    else throw new RuntimeException("Ran out of keys. Please add more keys")
  }
  private def isAlreadyEndorsed(did: PrismDid): Boolean = {
    validIn(did).lastOption.exists(_.to.isEmpty)
  }
  private def updatedValidInterval(did: PrismDid, timestamp: Instant): List[ValidInterval] = {
    val periods = validIn(did)
    val newValidInterval = periods.last.copy(to = Some(timestamp))
    periods.init :+ newValidInterval
  }

  // management related api
  def initialize(initialPrismDid: PrismDid, keys: List[SignedKey]): Future[Unit] =
    Future {
      moePrismDid = initialPrismDid
      signedKeys = keys
      trustedPrismDids = Set(initialPrismDid)
    }

  def getMoEPrismDid(): Future[PrismDid] = Future.successful(moePrismDid)

  // API
  def getFreshMasterKey(request: GetFreshMasterKeyRequest): Future[GetFreshMasterKeyResponse] = {
    Future.successful {
      val requester: PrismDid = PrismDid.fromString(request.endorserDID)
      val signedKey = nextKey()
      requestedBy = requestedBy.updated(signedKey.key, requester)
      println(s"assigned key: ${signedKey.key}")
      GetFreshMasterKeyResponse()
        .withKey(publicKeyToProto(signedKey.key))
        .withSignature(ByteString.copyFrom(signedKey.signature.getData))
        .withSigningKeyId(signedKey.signingKeyId)
    }
  }

  def endorseInstitution(request: EndorseInstitutionRequest): Future[EndorseInstitutionResponse] =
    Future {
      val parentPrismDid: PrismDid = PrismDid.fromString(request.parentDID)
      val childPrismDid: PrismDid = PrismDid.fromString(request.childDID)
      val signedOperation: SignedAtalaOperation = request.getIssueBatch

      val response = nodeServiceStub.getDidDocument(
        GetDidDocumentRequest(childPrismDid.toString)
      )
      val childMasterKeyList =
        response.getDocument.publicKeys.filter(k => k.usage == KeyUsage.MASTER_KEY && k.revokedOn.isEmpty)
      val childMasterKey =
        ProtoCodecs.fromProtoKey(childMasterKeyList.head).getOrElse(throw new RuntimeException("Failed to parse key"))

      val parentAssociatedToKey = requestedBy.getOrElse(childMasterKey, throw new RuntimeException("unknown key"))

      val credential = JsonBasedCredential.fromString(request.credential)
      val credentialPrismDid = Option(credential.getContent.getIssuerDid).get
      val operationPrismDid =
        PrismDid.fromString(signedOperation.getOperation.getIssueCredentialBatch.getCredentialBatchData.issuerDid)
      val operationMerkleRoot = new MerkleRoot(
        SHA256Digest.fromBytes(
          signedOperation.getOperation.getIssueCredentialBatch.getCredentialBatchData.merkleRoot.toByteArray
        )
      )
      val decodedProof = MerkleInclusionProof.decode(request.encodedMerkleProof)
      val proofDerivedRoot = decodedProof.derivedRoot

      if (
        // there should be a check that the parentPrismDid represents a role that can onboard the child PrismDid

        // tne child institution has only one active master key
        childMasterKeyList.size == 1 &&
        // the key was requested by the parent institution
        parentPrismDid == parentAssociatedToKey &&
        // the credential issuer matches the requester PrismDid
        parentPrismDid == credentialPrismDid &&
        // the parent PrismDid is the same than the one signing the operation
        parentPrismDid == operationPrismDid &&
        // the credential is included in the issuing operation
        operationMerkleRoot == proofDerivedRoot &&
        CredentialBatches.verifyInclusion(credential, operationMerkleRoot, decodedProof) &&
        // the PrismDid is not already endorsed
        !isAlreadyEndorsed(childPrismDid)
      ) {

        //if all checks are valid we issue the credential
        nodeServiceStub
          .issueCredentialBatch(
            IssueCredentialBatchRequest().withSignedOperation(signedOperation)
          )

        val interval = ValidInterval(
          from = Instant.now(),
          to = None,
          verifiableCredential = request.credential,
          inclusionProof = request.encodedMerkleProof
        )

        trustedPrismDids = trustedPrismDids + childPrismDid
        endorsedBy = endorsedBy.updated(childPrismDid, parentPrismDid)
        keyAssigned = keyAssigned.updated(childPrismDid, childMasterKey)
        validIn = validIn.updated(childPrismDid, validIn.getOrElse(childPrismDid, Nil) :+ interval)
        EndorseInstitutionResponse()
      } else {
        throw new RuntimeException("Endorsement validation failed")
      }
    }

  def getEndorsements(request: GetEndorsementsRequest): Future[GetEndorsementsResponse] =
    Future {
      val did = PrismDid.fromString(request.did)
      val intervals = validIn(did).map { interval =>
        ValidityInterval(to = interval.to.map(_.toProtoTimestamp))
          .withFrom(interval.from.toProtoTimestamp)
          .withCredential(interval.verifiableCredential)
          .withEncodedMerkleProof(interval.inclusionProof)
      }
      GetEndorsementsResponse()
        .withIntervals(intervals)
    }

  def revokeEndorsement(request: RevokeEndorsementRequest): Future[RevokeEndorsementResponse] =
    Future {
      val parentPrismDid = PrismDid.fromString(request.parentDID)
      val childPrismDid = PrismDid.fromString(request.childDID)
      val revokeOperation = request.getRevokeBatch

      if (endorsedBy(childPrismDid) == parentPrismDid) {
        nodeServiceStub.revokeCredentials(
          RevokeCredentialsRequest()
            .withSignedOperation(revokeOperation)
        )

        val revocationTime = Instant.now()
        trustedPrismDids = trustedPrismDids - childPrismDid
        validIn = validIn.updated(childPrismDid, updatedValidInterval(childPrismDid, revocationTime))
        RevokeEndorsementResponse()
      } else {
        throw new RuntimeException("Revocation failed")
      }
    }
}

object EndorsementsService {
  case class ValidInterval(
      from: Instant,
      to: Option[Instant],
      verifiableCredential: String,
      inclusionProof: String
  ) {
    def batchId: CredentialBatchId = {
      val issuerPrismDid = Try(
        JsonBasedCredential
          .fromString(verifiableCredential)
          .getContent
          .getIssuerDid
      ).getOrElse(throw new RuntimeException("missing issuer PrismDid"))
      CredentialBatchId.fromBatchData(
        issuerPrismDid.getSuffix,
        MerkleInclusionProof.decode(inclusionProof).derivedRoot
      )
    }
  }

  case class SignedKey(
      key: ECPublicKey,
      signature: ECSignature,
      signingKeyId: String
  )

  def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }
}
