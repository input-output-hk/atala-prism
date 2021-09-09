package io.iohk.atala.prism.connector

import cats.effect.IO

import java.util.UUID
import io.grpc.Context
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.connector.errors.UnknownValueError
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.{DIDUtil, auth}
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser}
import io.iohk.atala.prism.kotlin.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.kotlin.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.{connector_api, node_api, node_models}
import io.iohk.atala.prism.util.KeyUtils.createNodePublicKey
import org.mockito.ArgumentMatchersSugar._
import org.mockito.IdiomaticMockito._
import org.mockito.matchers.DefaultValueProvider
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class SignedRequestsAuthenticatorSpec extends AnyWordSpec {
  val defaultValueDID = new DefaultValueProvider[DID] {
    override def default: DID = DID.buildCanonical(Sha256Digest.fromHex("default"))
  }

  private implicit def patienceConfig: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  private val request = connector_api.GetConnectionTokenInfoRequest()
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
    val requestAuthenticator = new RequestAuthenticator

    "accept the public key authentication" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )

      testAuthentication(header)
    }

    "accept the unpublished did authentication" in {
      val (keys, unpublishedDid) = DIDUtil.createUnpublishedDid
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val header = GrpcAuthenticationHeader
        .UnpublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = unpublishedDid,
          DID.getMASTER_KEY_ID,
          signature = new ECSignature(signedRequest.signature)
        )

      testAuthentication(header)
    }

    "reject wrong public key authentication" in {
      val keys = EC.generateKeyPair()
      // signed with the wrong key
      val signedRequest =
        requestAuthenticator.signConnectorRequest(request.toByteArray, EC.generateKeyPair().getPrivateKey)
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[Exception] {
        result.futureValue
      }
    }

    "reject wrong nonce with public key authentication" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          // Different nonce
          requestNonce = auth.model.RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[Exception] {
        result.futureValue
      }
    }

    "reject public key authentication when reusing a nonce" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
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
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did.getValue,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublicKey))
          )
        )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => Some(nodeResponse))

      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      result.futureValue must be(response)
    }

    "reject wrong DID authentication" in {
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      // The request is signed with a different key
      val signedRequest =
        requestAuthenticator.signConnectorRequest(request.toByteArray, EC.generateKeyPair().getPrivateKey)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did.getValue,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublicKey))
          )
        )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator(getHeader = () => Some(header), getDidResponse = () => Some(nodeResponse))
      val result = authenticator.authenticated("test", request) { _ =>
        Future.successful(response)
      }
      intercept[RuntimeException] {
        result.futureValue
      }
    }

    "reject wrong nonce in DID authentication" in {
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did.getValue,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublicKey))
          )
        )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          // Different nonce
          requestNonce = auth.model.RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          did = did,
          keyId = keyId,
          signature = new ECSignature(signedRequest.signature)
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
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val participantsRepository = mock[ParticipantsRepository[IO]]
      participantsRepository.findBy(any[DID](defaultValueDID)).returns {
        IO.pure(Left(UnknownValueError("did", "not found")))
      }

      val customParser = new GrpcAuthenticationHeaderParser {
        override def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
          Some(
            GrpcAuthenticationHeader
              .PublishedDIDBased(
                requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
                did = did,
                keyId = keyId,
                signature = new ECSignature(signedRequest.signature)
              )
          )
        }
      }
      val authenticator = new ConnectorAuthenticator(
        participantsRepository,
        mock[RequestNoncesRepository[IO]],
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
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId,
          signature = new ECSignature(signedRequest.signature)
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
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did.getValue,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublicKey))
          )
        )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId + "1", // set a different key id
          signature = new ECSignature(signedRequest.signature)
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
      val did = DID.buildCanonical(Sha256Digest.fromHex("test"))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, keys.getPrivateKey)

      val nodeResponse = node_api
        .GetDidDocumentResponse()
        .withDocument(
          node_models.DIDData(
            id = did.getValue,
            publicKeys = List(createNodePublicKey(keyId, keys.getPublicKey))
          )
        )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId + "1", // set a different key id
          signature = new ECSignature(signedRequest.signature)
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

  // NOTE: To meet the repository interface we need to return the whole participant details while the authenticator
  // just uses the id, hence, hardcoding the values should be enough.
  private def dummyParticipantInfo(id: ParticipantId): ParticipantInfo = {
    ParticipantInfo(id = id, tpe = ParticipantType.Issuer, None, "no-name", None, None, None)
  }

  private def buildAuthenticator(
      getuserId: () => Option[ParticipantId] = () => Some(ParticipantId.random()),
      getHeader: () => Option[GrpcAuthenticationHeader],
      burnNonce: () => Unit = () => (),
      getDidResponse: () => Option[node_api.GetDidDocumentResponse] = () => None
  ): ConnectorAuthenticator = {
    val participantsRepository = mock[ParticipantsRepository[IO]]
    participantsRepository.findBy(any[DID](defaultValueDID)).returns {
      getuserId() match {
        case Some(userId) =>
          IO.pure(Right(dummyParticipantInfo(userId)))
        case None => IO.raiseError(new RuntimeException("Missing user"))
      }
    }

    participantsRepository.findBy(any[ECPublicKey]).returns {
      getuserId() match {
        case Some(userId) =>
          IO.pure(Right(dummyParticipantInfo(userId)))
        case None => IO.raiseError(new RuntimeException("Missing user"))
      }
    }

    val customParser = new GrpcAuthenticationHeaderParser {
      override def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
        getHeader()
      }
    }

    val requestNoncesRepository = mock[RequestNoncesRepository[IO]]
    requestNoncesRepository
      .burn(any[ParticipantId], any[auth.model.RequestNonce])
      .returns(IO(burnNonce()))

    val customNode = mock[NodeServiceGrpc.NodeService]
    customNode.getDidDocument(*).returns {
      getDidResponse() match {
        case Some(response) => Future.successful(response)
        case None => Future.failed(new RuntimeException("DID Document not found"))
      }
    }

    new ConnectorAuthenticator(participantsRepository, requestNoncesRepository, customNode, customParser)
  }

  private def testAuthentication(header: GrpcAuthenticationHeader): Assertion = {
    val authenticator = buildAuthenticator(getHeader = () => Some(header))

    val result = authenticator.authenticated("test", request) { _ =>
      Future.successful(response)
    }
    result.futureValue must be(response)
  }
}
