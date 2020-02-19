package io.iohk.connector

import java.security.PublicKey

import com.google.protobuf.ByteString
import io.iohk.connector.errors.UnknownValueError
import io.iohk.connector.model.{ConnectionId, ConnectionInfo, ParticipantInfo, TokenString}
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.connector.protos
import io.iohk.cvp.crypto.ECKeys.toEncodePublicKey
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.GrpcAuthenticationHeader
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.nodenew.node_api
import io.iohk.nodenew.node_api._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignedRequestsAuthenticatorSpec extends WordSpec {

  import SignedRequestsAuthenticatorSpec._

  private val request = protos.GetConnectionTokenInfoRequest("")
  private val response = protos.GetConnectionTokenInfoResponse()
  private val dummyNode = new DummyNodeService {}

  "public" should {
    val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {}, dummyNode)

    "accept the request without authentication" in {
      val result = authenticator.public("test", request) {
        Future.successful(response)
      }
      result.futureValue must be(response)
    }
  }

  "authenticated" should {
    "accept the legacy authentication" in {
      val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {}, dummyNode)
      val userId = ParticipantId.random()
      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] =
          Some(GrpcAuthenticationHeader.Legacy(userId))
      }
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      result.futureValue must be(response)
    }

    "reject wrong legacy authentication" in {
      val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {}, dummyNode)
      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = None
      }

      intercept[RuntimeException] {
        authenticator.authenticated("test", request) { _ =>
          Future.successful(response)
        }(global, customParser)
      }
    }

    "accept the public key authentication" in {
      val userId = ParticipantId.random()
      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(
            encodedPublicKey: ECKeys.EncodedPublicKey
        ): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, dummyNode)
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val encodedPublicKey = toEncodePublicKey(keys.getPublic)
      val signature = ECSignature.sign(privateKey, request.toByteArray)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.PublicKeyBased(publicKey = encodedPublicKey, signature = signature))
        }
      }
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      result.futureValue must be(response)
    }

    "reject wrong public key authentication" in {
      val userId = ParticipantId.random()
      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(
            encodedPublicKey: ECKeys.EncodedPublicKey
        ): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, dummyNode)
      val keys = ECKeys.generateKeyPair()
      val encodedPublicKey = toEncodePublicKey(keys.getPublic)
      // signed with the wrong key
      val signature = ECSignature.sign(ECKeys.generateKeyPair().getPrivate, request.toByteArray)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.PublicKeyBased(publicKey = encodedPublicKey, signature = signature))
        }
      }
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      intercept[Exception] {
        result.futureValue
      }
    }

    "accept the DID authentication" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val userId = ParticipantId.random()
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val signature = ECSignature.sign(privateKey, request.toByteArray)

      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_api.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val customNode = new DummyNodeService {
        override def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = {
          Future.successful(nodeResponse)
        }
      }
      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, customNode)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId, signature = signature))
        }
      }

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      result.futureValue must be(response)
    }

    "reject wrong DID authentication" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val userId = ParticipantId.random()
      val keys = ECKeys.generateKeyPair()
      // The request is signed with a different key
      val signature = ECSignature.sign(ECKeys.generateKeyPair().getPrivate, request.toByteArray)

      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_api.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val customNode = new DummyNodeService {
        override def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = {
          Future.successful(nodeResponse)
        }
      }
      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, customNode)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId, signature = signature))
        }
      }

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the did is not in our database" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val signature = ECSignature.sign(privateKey, request.toByteArray)

      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Left(UnknownValueError("did", "not found"))).toFutureEither
        }
      }

      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, dummyNode)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId, signature = signature))
        }
      }

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the did is not in the node" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val userId = ParticipantId.random()
      val keys = ECKeys.generateKeyPair()
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)

      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val customNode = new DummyNodeService {
        override def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = {
          Future.failed(new RuntimeException("DID not found"))
        }
      }
      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, customNode)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          Some(GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId, signature = signature))
        }
      }

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the key doesn't belong to the did" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val userId = ParticipantId.random()
      val keys = ECKeys.generateKeyPair()
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)

      val connectionsRepository = new DummyConnectionsRepository {
        override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = {
          Future.successful(Right(userId)).toFutureEither
        }
      }

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_api.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val customNode = new DummyNodeService {
        override def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = {
          Future.successful(nodeResponse)
        }
      }
      val authenticator = new SignedRequestsAuthenticator(connectionsRepository, customNode)

      val customParser = new Authenticator.RequestHeadersParser {
        override def parseAuthenticationHeader: Option[GrpcAuthenticationHeader] = {
          // set a different key id
          Some(GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId + "1", signature = signature))
        }
      }

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }(global, customParser)
      intercept[RuntimeException] {
        result.futureValue
      }
    }
  }

  private def createNodePublicKey(keyId: String, data: PublicKey): node_api.PublicKey = {
    val point = ECKeys.getECPoint(data)
    val x = ByteString.copyFrom(point.getAffineX.toByteArray)
    val y = ByteString.copyFrom(point.getAffineY.toByteArray)
    node_api.PublicKey(
      id = keyId,
      usage = node_api.KeyUsage.AUTHENTICATION_KEY,
      keyData = node_api.PublicKey.KeyData.EcKeyData(node_api.ECKeyData(curve = ECKeys.CURVE_NAME, x = x, y = y))
    )
  }
}

object SignedRequestsAuthenticatorSpec {
  trait DummyConnectionsRepository extends ConnectionsRepository {
    override def insertToken(initiator: ParticipantId, token: TokenString): FutureEither[Nothing, TokenString] = ???

    override def getTokenInfo(token: TokenString): FutureEither[errors.ConnectorError, ParticipantInfo] = ???

    override def getParticipantId(
        encodedPublicKey: ECKeys.EncodedPublicKey
    ): FutureEither[errors.ConnectorError, ParticipantId] = ???

    override def getParticipantId(did: String): FutureEither[errors.ConnectorError, ParticipantId] = ???

    override def addConnectionFromToken(
        token: TokenString,
        publicKey: ECKeys.EncodedPublicKey
    ): FutureEither[errors.ConnectorError, (ParticipantId, ConnectionInfo)] = ???

    override def getConnectionsPaginated(
        participant: ParticipantId,
        limit: Int,
        lastSeenConnectionId: Option[ConnectionId]
    ): FutureEither[errors.ConnectorError, Seq[ConnectionInfo]] = ???
  }

  trait DummyNodeService extends node_api.NodeServiceGrpc.NodeService {
    override def getDidDocument(request: GetDidDocumentRequest): Future[GetDidDocumentResponse] = ???

    override def createDID(request: SignedAtalaOperation): Future[CreateDIDResponse] = ???

    override def issueCredential(request: SignedAtalaOperation): Future[IssueCredentialResponse] = ???

    override def revokeCredential(request: SignedAtalaOperation): Future[RevokeCredentialResponse] = ???
  }
}
