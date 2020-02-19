package io.iohk.connector

import io.iohk.connector.model.{ConnectionId, ConnectionInfo, ParticipantInfo, TokenString}
import io.iohk.connector.repositories.ConnectionsRepository
import io.iohk.cvp.connector.protos
import io.iohk.cvp.crypto.ECKeys.toEncodePublicKey
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.GrpcAuthenticationHeader
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignedRequestsAuthenticatorSpec extends WordSpec {

  import SignedRequestsAuthenticatorSpec._

  private val request = protos.GetConnectionTokenInfoRequest("")
  private val response = protos.GetConnectionTokenInfoResponse()

  "public" should {
    val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {})

    "accept the request without authentication" in {
      val result = authenticator.public("test", request) {
        Future.successful(response)
      }
      result.futureValue must be(response)
    }
  }

  "authenticated" should {
    "accept the legacy authentication" in {
      val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {})
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
      val authenticator = new SignedRequestsAuthenticator(new DummyConnectionsRepository {})
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

      val authenticator = new SignedRequestsAuthenticator(connectionsRepository)
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

      val authenticator = new SignedRequestsAuthenticator(connectionsRepository)
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
      pending
    }

    "reject wrong DID authentication" in {
      pending
    }
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
}
