package io.iohk.atala.prism.connector

import cats.Id
import cats.data.ReaderT
import cats.effect.IO
import cats.effect.unsafe.implicits.{global => globalRuntime}
import io.grpc.Context
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.auth.errors.AuthErrorSupport
import io.iohk.atala.prism.auth.grpc.{GrpcAuthenticationHeader, GrpcAuthenticationHeaderParser}
import io.iohk.atala.prism.connector.errors.{UnknownValueError, co}
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.nonce.RequestAuthenticator
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.{connector_api, node_api, node_models}
import io.iohk.atala.prism.tracing.Tracing._
import io.iohk.atala.prism.util.KeyUtils.createNodePublicKey
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.{DIDUtil, auth}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.IdiomaticMockito._
import org.mockito.matchers.DefaultValueProvider
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}
import tofu.logging.Logs

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class SignedRequestsAuthenticatorSpec extends AnyWordSpec with AuthErrorSupport {
  val defaultValueDID = new DefaultValueProvider[DID] {
    override def default: DID =
      DID.buildCanonical(Sha256.compute("default".getBytes("UTF-8")))
  }
  val testLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  private implicit def patienceConfig: PatienceConfig =
    PatienceConfig(20.seconds, 50.millis)

  private val request = connector_api.GetConnectionTokenInfoRequest()
  private val response = connector_api.GetConnectionTokenInfoResponse()
  private val testTraceId = TraceId("testTraceId")

  "public" should {
    "accept the request without authentication" in {
      val authenticator = buildAuthenticator()
      val result = {
        for {
          _ <- authenticator.public("test", request).run(testTraceId).unsafeToFuture()
          resp <- Future.successful(response)
        } yield resp
      }
      result.futureValue must be(response)
    }

    "parse trace id from header" in {
      val externalTraceId = TraceId("exactlyThisTraceId123")

      val customParser = new GrpcAuthenticationHeaderParser {
        override def getTraceId(ctx: Context): TraceId = externalTraceId
      }

      val currentTraceId = trace[Id, TraceId](
        traceId => traceId,
        customParser
      )

      currentTraceId must be(externalTraceId)
    }

  }

  "authenticated" should {
    val requestAuthenticator = new RequestAuthenticator

    "accept the public key authentication" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

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
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

      val header = GrpcAuthenticationHeader
        .UnpublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = unpublishedDid,
          DID.getDEFAULT_MASTER_KEY_ID,
          signature = new ECSignature(signedRequest.signature)
        )

      testAuthentication(header)
    }

    "reject wrong public key authentication" in {
      val keys = EC.generateKeyPair()
      // signed with the wrong key
      val signedRequest =
        requestAuthenticator.signConnectorRequest(
          request.toByteArray,
          EC.generateKeyPair().getPrivateKey
        )
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator()

      val result = authenticator
        .authenticated("test", request, Some(header))
        .run(testTraceId)
        .unsafeRunSync()

      result.isLeft must be(true)
    }

    "reject wrong nonce with public key authentication" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          // Different nonce
          requestNonce = auth.model.RequestNonce(UUID.randomUUID.toString.getBytes.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )

      val authenticator = buildAuthenticator()

      val result = authenticator
        .authenticated("test", request, Some(header))
        .run(testTraceId)
        .unsafeRunSync()

      result.isLeft must be(true)
    }

    "reject public key authentication when reusing a nonce" in {
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )
      val header = GrpcAuthenticationHeader
        .PublicKeyBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          publicKey = keys.getPublicKey,
          signature = new ECSignature(signedRequest.signature)
        )
      val authenticator =
        buildAuthenticator(
          burnNonce = () => throw new RuntimeException("Nonce reused")
        )

      def result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      intercept[Exception] {
        result
      }
    }

    "accept the DID authentication" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

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

      val authenticator = buildAuthenticator(
        getDidResponse = () => Some(nodeResponse)
      )

      val result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      result.isRight must be(true)
    }

    "reject wrong DID authentication" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      // The request is signed with a different key
      val signedRequest =
        requestAuthenticator.signConnectorRequest(
          request.toByteArray,
          EC.generateKeyPair().getPrivateKey
        )

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

      val authenticator = buildAuthenticator(
        getDidResponse = () => Some(nodeResponse)
      )
      val result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      result.isLeft must be(true)

    }

    "reject wrong nonce in DID authentication" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

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

      val authenticator = buildAuthenticator(
        getDidResponse = () => Some(nodeResponse)
      )
      val result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()

      result.isLeft must be(true)

    }

    "fail when the did is not in our database" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

      val participantsRepository =
        mock[ParticipantsRepository[IOWithTraceIdContext]]
      participantsRepository.findBy(any[DID](defaultValueDID)).returns {
        ReaderT.liftF(IO.pure(Left(co(UnknownValueError("did", "not found")))))
      }

      val header = Some(
        GrpcAuthenticationHeader
          .PublishedDIDBased(
            requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
            did = did,
            keyId = keyId,
            signature = new ECSignature(signedRequest.signature)
          )
      )

      val authenticator = AuthenticatorF.unsafe(
        mock[NodeServiceGrpc.NodeService],
        new ConnectorAuthenticator(
          participantsRepository,
          mock[RequestNoncesRepository[IOWithTraceIdContext]]
        ),
        testLogs
      )

      val result = authenticator.authenticated("test", request, header).run(testTraceId).unsafeRunSync()
      result.isLeft must be(true)
    }

    "fail when the did is not in the node" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

      val header = GrpcAuthenticationHeader
        .PublishedDIDBased(
          requestNonce = auth.model.RequestNonce(signedRequest.requestNonce.toVector),
          did = did,
          keyId = keyId,
          signature = new ECSignature(signedRequest.signature)
        )
      val authenticator = buildAuthenticator(
        getDidResponse = () => None
      )

      def result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      intercept[RuntimeException] {
        result
      }
    }

    "fail when the key doesn't belong to the did" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

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
        getDidResponse = () => Some(nodeResponse)
      )
      val result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      result.isLeft must be(true)
    }

    "fail when the nonce is reused" in {
      val did = DID.buildCanonical(Sha256.compute("test".getBytes("UTF-8")))
      val keyId = "key-1"
      val keys = EC.generateKeyPair()
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.toByteArray,
        keys.getPrivateKey
      )

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
        getDidResponse = () => Some(nodeResponse),
        burnNonce = () => throw new RuntimeException("Nonce already used")
      )
      def result = authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeRunSync()
      result.isLeft must be(true)
    }
  }

  // NOTE: To meet the repository interface we need to return the whole participant details while the authenticator
  // just uses the id, hence, hardcoding the values should be enough.
  private def dummyParticipantInfo(id: ParticipantId): ParticipantInfo = {
    ParticipantInfo(
      id = id,
      tpe = ParticipantType.Issuer,
      None,
      "no-name",
      None,
      None,
      None
    )
  }

  private def buildAuthenticator(
      getuserId: () => Option[ParticipantId] = () => Some(ParticipantId.random()),
      burnNonce: () => Unit = () => (),
      getDidResponse: () => Option[node_api.GetDidDocumentResponse] = () => None
  ): AuthenticatorF[ParticipantId, IOWithTraceIdContext] = {
    val participantsRepository =
      mock[ParticipantsRepository[IOWithTraceIdContext]]
    participantsRepository.findBy(any[DID](defaultValueDID)).returns {
      ReaderT.liftF(getuserId() match {
        case Some(userId) => IO.pure(Right(dummyParticipantInfo(userId)))
        case None => IO.raiseError(new RuntimeException("Missing user"))
      })
    }

    participantsRepository.findBy(any[ECPublicKey]).returns {
      ReaderT.liftF(getuserId() match {
        case Some(userId) => IO.pure(Right(dummyParticipantInfo(userId)))
        case None => IO.raiseError(new RuntimeException("Missing user"))
      })
    }

    val requestNoncesRepository =
      mock[RequestNoncesRepository[IOWithTraceIdContext]]
    requestNoncesRepository
      .burn(any[ParticipantId], any[auth.model.RequestNonce])
      .returns(ReaderT.liftF(IO(burnNonce())))

    val customNode = mock[NodeServiceGrpc.NodeService]
    customNode.getDidDocument(*).returns {
      getDidResponse() match {
        case Some(response) => Future.successful(response)
        case None =>
          Future.failed(new RuntimeException("DID Document not found"))
      }
    }

    AuthenticatorF.unsafe(
      customNode,
      new ConnectorAuthenticator(
        participantsRepository,
        requestNoncesRepository
      ),
      testLogs
    )
  }

  private def testAuthentication(
      header: GrpcAuthenticationHeader
  ): Assertion = {
    val authenticator = buildAuthenticator()

    val result = (for {
      _ <- authenticator.authenticated("test", request, Some(header)).run(testTraceId).unsafeToFuture().toFutureEither
      resp <- Future.successful(Right(response)).toFutureEither
    } yield resp).flatten

    result.futureValue must be(response)
  }

  override def logger: Logger = LoggerFactory.getLogger(this.getClass)
}
