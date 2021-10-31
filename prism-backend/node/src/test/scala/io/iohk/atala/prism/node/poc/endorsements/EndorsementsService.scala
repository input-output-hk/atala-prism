package io.iohk.atala.prism.node.poc.endorsements

import java.time.Instant
import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.crypto.{MerkleInclusionProof, MerkleRoot, Sha256Digest}
import io.iohk.atala.prism.api.CredentialBatches
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
  private var moeDID: DID = _
  private var trustedDIDs: Set[DID] = Set.empty
  private var signedKeys: List[SignedKey] = List.empty
  private var requestedBy: Map[ECPublicKey, DID] = Map.empty
  private var endorsedBy: Map[DID, DID] = Map.empty
  private var keyAssigned: Map[DID, ECPublicKey] = Map.empty
  private var validIn: Map[DID, List[ValidInterval]] =
    Map.empty.withDefaultValue(Nil)

  private var lastRequested: Int = -1
  private def nextKey(): SignedKey = {
    lastRequested += 1
    if (lastRequested < signedKeys.size) signedKeys(lastRequested)
    else throw new RuntimeException("Ran out of keys. Please add more keys")
  }
  private def isAlreadyEndorsed(did: DID): Boolean = {
    validIn(did).lastOption.exists(_.to.isEmpty)
  }
  private def updatedValidInterval(
      did: DID,
      timestamp: Instant
  ): List[ValidInterval] = {
    val periods = validIn(did)
    val newValidInterval = periods.last.copy(to = Some(timestamp))
    periods.init :+ newValidInterval
  }

  // management related api
  def initialize(initialDID: DID, keys: List[SignedKey]): Future[Unit] =
    Future {
      moeDID = initialDID
      signedKeys = keys
      trustedDIDs = Set(initialDID)
    }

  def getMoEDID(): Future[DID] = Future.successful(moeDID)

  // API
  def getFreshMasterKey(
      request: GetFreshMasterKeyRequest
  ): Future[GetFreshMasterKeyResponse] = {
    Future.successful {
      val requester: DID = DID.fromString(request.endorserDID)
      val signedKey = nextKey()
      requestedBy = requestedBy.updated(signedKey.key, requester)
      println(s"assigned key: ${signedKey.key}")
      GetFreshMasterKeyResponse()
        .withKey(publicKeyToProto(signedKey.key))
        .withSignature(ByteString.copyFrom(signedKey.signature.getData))
        .withSigningKeyId(signedKey.signingKeyId)
    }
  }

  def endorseInstitution(
      request: EndorseInstitutionRequest
  ): Future[EndorseInstitutionResponse] =
    Future {
      val parentDID: DID = DID.fromString(request.parentDID)
      val childDID: DID = DID.fromString(request.childDID)
      val signedOperation: SignedAtalaOperation = request.getIssueBatch

      val response = nodeServiceStub.getDidDocument(
        GetDidDocumentRequest(childDID.toString)
      )
      val childMasterKeyList =
        response.getDocument.publicKeys.filter(k => k.usage == KeyUsage.MASTER_KEY && k.revokedOn.isEmpty)
      val childMasterKey =
        ProtoCodecs
          .fromProtoKey(childMasterKeyList.head)
          .getOrElse(throw new RuntimeException("Failed to parse key"))

      val parentAssociatedToKey = requestedBy.getOrElse(
        childMasterKey,
        throw new RuntimeException("unknown key")
      )

      val credential = JsonBasedCredential.fromString(request.credential)
      val credentialDID = Option(credential.getContent.getIssuerDid).get
      val operationDID =
        DID.buildCanonical(
          Sha256Digest.fromHex(
            signedOperation.getOperation.getIssueCredentialBatch.getCredentialBatchData.issuerDid
          )
        )
      val operationMerkleRoot = new MerkleRoot(
        Sha256Digest.fromBytes(
          signedOperation.getOperation.getIssueCredentialBatch.getCredentialBatchData.merkleRoot.toByteArray
        )
      )
      val decodedProof = MerkleInclusionProof.decode(request.encodedMerkleProof)
      val proofDerivedRoot = decodedProof.derivedRoot

      if (
        // there should be a check that the parentDID represents a role that can onboard the child DID

        // tne child institution has only one active master key
        childMasterKeyList.size == 1 &&
        // the key was requested by the parent institution
        parentDID == parentAssociatedToKey &&
        // the credential issuer matches the requester DID
        parentDID == credentialDID &&
        // the parent DID is the same than the one signing the operation
        parentDID == operationDID &&
        // the credential is included in the issuing operation
        operationMerkleRoot == proofDerivedRoot &&
        CredentialBatches.verifyInclusion(
          credential,
          operationMerkleRoot,
          decodedProof
        ) &&
        // the DID is not already endorsed
        !isAlreadyEndorsed(childDID)
      ) {

        // if all checks are valid we issue the credential
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

        trustedDIDs = trustedDIDs + childDID
        endorsedBy = endorsedBy.updated(childDID, parentDID)
        keyAssigned = keyAssigned.updated(childDID, childMasterKey)
        validIn = validIn.updated(
          childDID,
          validIn.getOrElse(childDID, Nil) :+ interval
        )
        EndorseInstitutionResponse()
      } else {
        throw new RuntimeException("Endorsement validation failed")
      }
    }

  def getEndorsements(
      request: GetEndorsementsRequest
  ): Future[GetEndorsementsResponse] =
    Future {
      val did = DID.fromString(request.did)
      val intervals = validIn(did).map { interval =>
        ValidityInterval(to = interval.to.map(_.toProtoTimestamp))
          .withFrom(interval.from.toProtoTimestamp)
          .withCredential(interval.verifiableCredential)
          .withEncodedMerkleProof(interval.inclusionProof)
      }
      GetEndorsementsResponse()
        .withIntervals(intervals)
    }

  def revokeEndorsement(
      request: RevokeEndorsementRequest
  ): Future[RevokeEndorsementResponse] =
    Future {
      val parentDID = DID.fromString(request.parentDID)
      val childDID = DID.fromString(request.childDID)
      val revokeOperation = request.getRevokeBatch

      if (endorsedBy(childDID) == parentDID) {
        nodeServiceStub.revokeCredentials(
          RevokeCredentialsRequest()
            .withSignedOperation(revokeOperation)
        )

        val revocationTime = Instant.now()
        trustedDIDs = trustedDIDs - childDID
        validIn = validIn.updated(
          childDID,
          updatedValidInterval(childDID, revocationTime)
        )
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
      val issuerDID = Try(
        JsonBasedCredential
          .fromString(verifiableCredential)
          .getContent
          .getIssuerDid
      ).getOrElse(throw new RuntimeException("missing issuer DID"))
      CredentialBatchId.fromBatchData(
        issuerDID.getSuffix,
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
