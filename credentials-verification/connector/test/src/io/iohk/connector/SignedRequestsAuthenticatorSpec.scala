package io.iohk.connector

import java.security.PublicKey
import java.util.UUID

import com.google.protobuf.ByteString
import io.grpc.Context
import io.iohk.connector.errors.UnknownValueError
import io.iohk.connector.model._
import io.iohk.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.cvp.crypto.ECKeys.{EncodedPublicKey, toEncodedPublicKey}
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser, SignedRequestsHelper}
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.prism.protos.node_api._
import io.iohk.prism.protos.{connector_api, node_api, node_models}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.IdiomaticMockito._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignedRequestsAuthenticatorSpec extends WordSpec {

  private val request = connector_api.GetConnectionTokenInfoRequest("")
  private val response = connector_api.GetConnectionTokenInfoResponse()

  "public" should {
    "accept the request without authentication" in {
      val authenticator = buildAuthenticator(getHeader = () => None)
      val result = authenticator.public("test", request) {
        Future.successful(response)
      }
      result.futureValue must be(response)
    }
  }

  "authenticated" should {
    "accept the legacy authentication" in {
      val header = GrpcAuthenticationHeader.Legacy(ParticipantId.random())
      val authenticator = buildAuthenticator(getHeader = () => Some(header))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      result.futureValue must be(response)
    }

    "reject wrong legacy authentication" in {
      val authenticator = buildAuthenticator(getHeader = () => None)

      intercept[RuntimeException] {
        authenticator
          .authenticated("test", request) { _ =>
            Future.successful(response)
          }
          .futureValue
      }
    }

    "accept the public key authentication" in {
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      val requestNonce = UUID.randomUUID.toString.getBytes.toVector
      val signature =
        ECSignature.sign(
          privateKey,
          SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
        )

      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = RequestNonce(requestNonce),
          publicKey = encodedPublicKey,
          signature = signature
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      result.futureValue must be(response)
    }

    "reject wrong public key authentication" in {
      val keys = ECKeys.generateKeyPair()
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      // signed with the wrong key
      val signature = ECSignature.sign(ECKeys.generateKeyPair().getPrivate, request.toByteArray)
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          publicKey = encodedPublicKey,
          signature = signature
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[Exception] {
        result.futureValue
      }
    }

    "reject public key authentication when the reusing a nonce" in {
      val keys = ECKeys.generateKeyPair()
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          publicKey = encodedPublicKey,
          signature = signature
        )
      val authenticator =
        buildAuthenticator(getHeader = () => Some(header), burnNonce = () => throw new RuntimeException("Nonce reused"))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[Exception] {
        result.futureValue
      }
    }

    "accept the DID authentication" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val requestNonce = UUID.randomUUID.toString.getBytes.toVector
      val signature = ECSignature.sign(
        privateKey,
        SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
      )

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val header = GrpcAuthenticationHeader
        .DIDBased(
          requestNonce = RequestNonce(requestNonce),
          did = did,
          keyId = keyId,
          signature = signature
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => Some(nodeResponse))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      result.futureValue must be(response)
    }

    "reject wrong DID authentication" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      // The request is signed with a different key
      val signature = ECSignature.sign(ECKeys.generateKeyPair().getPrivate, request.toByteArray)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val header = GrpcAuthenticationHeader
        .DIDBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          did = did,
          keyId = keyId,
          signature = signature
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => Some(nodeResponse))
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
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

      val participantsRepository = mock[ParticipantsRepository]
      participantsRepository.findBy(any[String]).returns {
        Future.successful(Left(UnknownValueError("did", "not found"))).toFutureEither
      }

      val customParser = new GrpcAuthenticationHeaderParser {
        override def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
          Some(
            GrpcAuthenticationHeader
              .DIDBased(
                RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
                did = did,
                keyId = keyId,
                signature = signature
              )
          )
        }
      }
      val authenticator = new SignedRequestsAuthenticator(
        participantsRepository,
        mock[RequestNoncesRepository],
        mock[NodeServiceGrpc.NodeService],
        customParser
      )

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the did is not in the node" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)

      val header = GrpcAuthenticationHeader
        .DIDBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          did = did,
          keyId = keyId,
          signature = signature
        )
      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => None)

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the key doesn't belong to the did" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val header = GrpcAuthenticationHeader
        .DIDBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          did = did,
          keyId = keyId + "1", // set a different key id
          signature = signature
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => Some(nodeResponse))
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "fail when the nonce is reused" in {
      val did = "did:prism:test"
      val keyId = "key-1"
      val keys = ECKeys.generateKeyPair()
      val signature = ECSignature.sign(keys.getPrivate, request.toByteArray)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublic))
          )
        )

      val header = GrpcAuthenticationHeader
        .DIDBased(
          RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          did = did,
          keyId = keyId + "1", // set a different key id
          signature = signature
        )

      val authenticator = buildAuthenticator(
        getHeader = () => Some(header),
        getDidResponse = () => Some(nodeResponse),
        burnNonce = () => throw new RuntimeException("Nonce already used")
      )
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[RuntimeException] {
        result.futureValue
      }
    }
  }

  private def createNodePublicKey(keyId: String, data: PublicKey): node_models.PublicKey = {
    val point = ECKeys.getECPoint(data)
    val x = ByteString.copyFrom(point.getAffineX.toByteArray)
    val y = ByteString.copyFrom(point.getAffineY.toByteArray)
    node_models.PublicKey(
      id = keyId,
      usage = node_models.KeyUsage.AUTHENTICATION_KEY,
      keyData = node_models.PublicKey.KeyData.EcKeyData(node_models.ECKeyData(curve = ECKeys.CURVE_NAME, x = x, y = y))
    )
  }

  // NOTE: To meet the repository interface we need to return the whole participant details while the authenticator
  // just uses the id, hence, hardcoding the values should be enough.
  private def dummyParticipantInfo(id: ParticipantId): ParticipantInfo = {
    ParticipantInfo(id = id, tpe = ParticipantType.Issuer, None, "no-name", None, None)
  }

  private def buildAuthenticator(
      getuserId: () => Option[ParticipantId] = () => Some(ParticipantId.random()),
      getHeader: () => Option[GrpcAuthenticationHeader],
      burnNonce: () => Unit = () => (),
      getDidResponse: () => Option[node_api.GetDidDocumentResponse] = () => None
  ): Authenticator = {
    val participantsRepository = mock[ParticipantsRepository]
    participantsRepository.findBy(any[String]).returns {
      getuserId() match {
        case Some(userId) =>
          Future.successful(Right(dummyParticipantInfo(userId))).toFutureEither
        case None => Future.failed(new RuntimeException("Missing user")).toFutureEither
      }
    }

    participantsRepository.findBy(any[EncodedPublicKey]).returns {
      getuserId() match {
        case Some(userId) =>
          Future.successful(Right(dummyParticipantInfo(userId))).toFutureEither
        case None => Future.failed(new RuntimeException("Missing user")).toFutureEither
      }
    }

    val customParser = new GrpcAuthenticationHeaderParser {
      override def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
        getHeader()
      }
    }

    val requestNoncesRepository = mock[RequestNoncesRepository]
    requestNoncesRepository
      .burn(any[ParticipantId], any[RequestNonce])
      .returns(Future(burnNonce()).map(Right(_)).toFutureEither)

    val customNode = mock[NodeServiceGrpc.NodeService]
    customNode.getDidDocument(*).returns {
      getDidResponse() match {
        case Some(response) => Future.successful(response)
        case None => Future.failed(new RuntimeException("DID Document not found"))
      }
    }

    new SignedRequestsAuthenticator(participantsRepository, requestNoncesRepository, customNode, customParser)
  }
}
