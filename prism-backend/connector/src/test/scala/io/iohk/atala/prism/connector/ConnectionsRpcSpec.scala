package io.iohk.atala.prism.connector

import cats.syntax.option._
import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.connector.model.ParticipantType.Holder
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.atala.prism.crypto.{EC, ECConfig}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.node_api.GetDidDocumentRequest
import io.iohk.atala.prism.protos.{connector_api, connector_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Random

class ConnectionsRpcSpec extends ConnectorRpcSpecBase with MockitoSugar {

  "GenerateConnectionTokens" should {
    "generate connection tokens" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val tokensCount = 3
      val _ = createIssuer("Issuer", Some(publicKey), Some(did))
      val request = connector_api.GenerateConnectionTokenRequest(tokensCount)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.generateConnectionToken(request)
        val tokens = response.tokens.map(new TokenString(_))

        tokens.size mustBe tokensCount
        tokens.foreach { token =>
          ConnectionTokensDAO
            .exists(token)
            .transact(database)
            .unsafeToFuture()
            .futureValue mustBe true
        }
      }
    }

    "generate connection tokens when tokens count is not supplied" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createIssuer("Issuer", Some(publicKey), Some(did))
      val request = connector_api.GenerateConnectionTokenRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.generateConnectionToken(request)
        val tokens = response.tokens.map(new TokenString(_))

        tokens.size mustBe 1
        ConnectionTokensDAO
          .exists(tokens.head)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true
      }
    }
  }

  "GetConnectionTokenInfo" should {
    "return token info" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer("Issuer", Some(publicKey), Some(did))
      val token = createToken(issuerId)
      val request = connector_api.GetConnectionTokenInfoRequest(token.token)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getConnectionTokenInfo(request)
        response.creatorName mustBe "Issuer"
        response.creatorLogo.size() must be > 0 // the issuer has a logo
        response.creatorDid mustBe did.toString
      }
    }

    "returns UNKNOWN if token does not exist" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createIssuer("Issuer", Some(publicKey), Some(did))
      val token = TokenString.random()
      val request = connector_api.GetConnectionTokenInfoRequest(token.token)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionTokenInfo(request)
        }.getStatus

        status.getCode mustBe Status.Code.UNKNOWN
        status.getDescription must include(token.token)
      }
    }
  }

  "AddConnectionFromToken" should {
    "add connection from token" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(ByteString.copyFrom(keys.publicKey.getEncoded))
      val request = connector_api
        .AddConnectionFromTokenRequest(token.token)
        .withHolderEncodedPublicKey(encodedPubKey)
      usingApiAs(Vector.empty, keys, request) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(request)
        val holderId = response.userId
        holderId mustNot be(empty)
        response.connection.value.participantName mustBe "Issuer"
        val connectionId = ConnectionId.unsafeFrom(response.connection.value.connectionId)

        val participantInfo = io.iohk.atala.prism.connector.model.ParticipantInfo(
          ParticipantId.unsafeFrom(holderId),
          Holder,
          Some(keys.publicKey),
          "",
          None,
          None,
          None,
          None
        )
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        ParticipantsDAO
          .findByPublicKey(keys.publicKey)
          .transact(database)
          .value
          .unsafeToFuture()
          .futureValue mustBe Some(participantInfo)
      }
    }

    "add connection from token using unpublished did auth" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = EC.generateKeyPair()
      val unpublishedDID = DID.createUnpublishedDID(keys.publicKey)
      val request = connector_api
        .AddConnectionFromTokenRequest(token.token)
      val rpcRequest = SignedRpcRequest.generate(keys, unpublishedDID, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(request)
        val holderId = response.userId
        holderId mustNot be(empty)
        val connectionId = ConnectionId.unsafeFrom(response.connection.value.connectionId)

        val participantInfo = io.iohk.atala.prism.connector.model.ParticipantInfo(
          ParticipantId.unsafeFrom(holderId),
          Holder,
          None,
          "",
          unpublishedDID.canonical,
          None,
          None,
          None
        )
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        ParticipantsDAO
          .findByDID(unpublishedDID)
          .transact(database)
          .value
          .unsafeToFuture()
          .futureValue mustBe Some(participantInfo)
      }
    }

    "fail to add connection when signature missing" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(ByteString.copyFrom(keys.publicKey.getEncoded))
      usingApiAs.unlogged { blockingStub =>
        val request = connector_api
          .AddConnectionFromTokenRequest(token.token)
          .withHolderEncodedPublicKey(encodedPubKey)
        val ex = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request)
        }
        ex.getStatus.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return ALREADY_EXISTS if the same DID is used to connect with an issuer twice" in {
      val holderKeys = EC.generateKeyPair()
      val issuer = createIssuer("Issuer")
      val holderDID = DID.createUnpublishedDID(holderKeys.publicKey)
      val tokenStr1 = createToken(issuer)
      val tokenStr2 = createToken(issuer)
      val encodedHolderPublicKey =
        connector_models.EncodedPublicKey(ByteString.copyFrom(holderKeys.publicKey.getEncoded))

      val request1 = connector_api.AddConnectionFromTokenRequest(tokenStr1.token, Some(encodedHolderPublicKey))
      val request2 = connector_api.AddConnectionFromTokenRequest(tokenStr2.token, Some(encodedHolderPublicKey))

      val signedRequest1 = SignedRpcRequest.generate(holderKeys, holderDID, request1)
      val signedRequest2 = SignedRpcRequest.generate(holderKeys, holderDID, request2)

      usingApiAs(signedRequest1) { blockingStub =>
        // first connection should be established
        val response = blockingStub.addConnectionFromToken(request1)
        val participantId = response.userId
        participantId mustNot be(empty)
        val connectionId = ConnectionId.unsafeFrom(response.connection.value.connectionId)
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true
      }
      usingApiAs(signedRequest2) { blockingStub =>
        // second connection should fail
        val status = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request2)
        }.getStatus
        status.getCode mustBe Status.Code.ALREADY_EXISTS
        status.getDescription must include(holderDID.toString)
      }

    }

    "return ALREADY_EXISTS if the same Public key is used to connect with an issuer twice" in {
      val holderKeys = EC.generateKeyPair()
      val issuer = createIssuer("Issuer")
      val tokenStr1 = createToken(issuer)
      val tokenStr2 = createToken(issuer)
      val encodedHolderPublicKey =
        connector_models.EncodedPublicKey(ByteString.copyFrom(holderKeys.publicKey.getEncoded))

      val request1 = connector_api.AddConnectionFromTokenRequest(tokenStr1.token, Some(encodedHolderPublicKey))
      val request2 = connector_api.AddConnectionFromTokenRequest(tokenStr2.token, Some(encodedHolderPublicKey))

      usingApiAs(Vector.empty, holderKeys, request1) { blockingStub =>
        // first connection should be established
        val response = blockingStub.addConnectionFromToken(request1)
        val participantId = response.userId
        participantId mustNot be(empty)
        val connectionId = ConnectionId.unsafeFrom(response.connection.value.connectionId)
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true
      }
      usingApiAs(Vector.empty, holderKeys, request2) { blockingStub =>
        // second connection should fail
        val status = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request2)
        }.getStatus
        status.getCode mustBe Status.Code.ALREADY_EXISTS
        status.getDescription must include(holderKeys.publicKey.toString)
      }

    }

    "return UNKNOWN if the token does not exist" in {
      val token = TokenString.random()
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(ByteString.copyFrom(keys.publicKey.getEncoded))

      val request = connector_api
        .AddConnectionFromTokenRequest(token.token)
        .withHolderEncodedPublicKey(encodedPubKey)
      usingApiAs(Vector.empty, keys, request) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request)
        }.getStatus

        status.getCode mustBe Status.Code.UNKNOWN
        status.getDescription must include(token.token)
      }
    }
  }

  "RevokeConnection" should {
    "work" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(ByteString.copyFrom(keys.publicKey.getEncoded))
      val addTokenRequest = connector_api
        .AddConnectionFromTokenRequest(token.token)
        .withHolderEncodedPublicKey(encodedPubKey)

      // create connection
      val connectionId = usingApiAs(Random.nextBytes(80).toVector, keys, addTokenRequest) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(addTokenRequest)
        val connectionId = ConnectionId.unsafeFrom(response.connection.value.connectionId)
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        connectionId
      }

      // revoke connection
      val revokeRequest = connector_api
        .RevokeConnectionRequest()
        .withConnectionId(connectionId.uuid.toString)

      usingApiAs(Random.nextBytes(80).toVector, keys, revokeRequest) { blockingStub =>
        blockingStub.revokeConnection(revokeRequest)
        val existing = ConnectionsDAO
          .getRawConnection(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue
          .value

        existing.status must be(ConnectionStatus.ConnectionRevoked)
      }
    }
  }

  "GetConnectionsPaginated" should {
    "return new connections" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val verifierId = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetConnectionsPaginatedRequest("", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      val zeroTime = System.currentTimeMillis()
      val connections = createExampleConnections(verifierId, zeroTime)

      val response = usingApiAs(rpcRequest)(_.getConnectionsPaginated(request))

      response.connections.map(_.connectionId).toSet mustBe connections.map(_._2.toString).take(10).toList.toSet

      val nextRequest = connector_api.GetConnectionsPaginatedRequest(response.connections.last.connectionId, 10)
      val nextRpcRequest = SignedRpcRequest.generate(keyPair, did, nextRequest)

      usingApiAs(nextRpcRequest) { blockingStub =>
        val nextResponse = blockingStub.getConnectionsPaginated(nextRequest)
        nextResponse.connections
          .map(_.connectionId)
          .toSet mustBe connections.map(_._2.toString).slice(10, 20).toList.toSet
      }
    }

    "return new connections authenticating by signature" in {
      val keys = EC.generateKeyPair()
      val privateKey = keys.privateKey
      val did = generateDid(keys.publicKey)
      val request = connector_api.GetConnectionsPaginatedRequest("", 10)
      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        EC.sign(
          SignedRequestsHelper.merge(auth.model.RequestNonce(requestNonce), request.toByteArray).toArray,
          privateKey
        )

      val verifierId = createVerifier("Verifier", Some(keys.publicKey), Some(did))

      val zeroTime = System.currentTimeMillis()
      val connections = createExampleConnections(verifierId, zeroTime)

      usingApiAs(requestNonce, signature, did, "master0") { blockingStub =>
        val response = blockingStub.getConnectionsPaginated(request)
        response.connections.map(_.connectionId).toSet mustBe connections.map(_._2.toString).take(10).toList.toSet
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetConnectionsPaginatedRequest("", 0)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when limit is negative" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetConnectionsPaginatedRequest("", -7)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when provided id is not a valid" in {
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val _ = createVerifier("Verifier", Some(publicKey), Some(did))
      val request = connector_api.GetConnectionsPaginatedRequest("uaoen", 10)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
        status.getDescription must include("uaoen")
      }
    }
  }

  "getConnectionCommunicationKeys" should {
    "return non-revoked keys for a DID owning participant" in {
      val earlierTimestamp = LocalDateTime.of(2020, 5, 12, 0, 0).toInstant(ZoneOffset.UTC)
      val laterTimestamp = LocalDateTime.of(2020, 5, 13, 0, 0).toInstant(ZoneOffset.UTC)
      val issuerCommKeys = Seq(
        ("foo", EC.generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY),
        ("bar", EC.generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY),
        ("revoked", EC.generateKeyPair(), Some(laterTimestamp), node_models.KeyUsage.COMMUNICATION_KEY),
        ("master", EC.generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY)
      )

      val holderKey = EC.generateKeyPair()
      val holderDID = generateDid(holderKey.publicKey)

      val issuerAuthKey = EC.generateKeyPair()
      val issuerDID = generateDid(EC.generateKeyPair().publicKey)
      val issuerId =
        createIssuer("Issuer", publicKey = Some(issuerAuthKey.publicKey), did = Some(issuerDID))
      val holderId = createHolder("Holder", publicKey = Some(holderKey.publicKey), did = Some(holderDID))
      val connectionId = createConnection(issuerId, holderId)

      val response = node_api.GetDidDocumentResponse(
        Some(
          node_models.DIDData(
            id = issuerDID.suffix.value,
            publicKeys = issuerCommKeys.map {
              case (keyId, key, revokedTimestamp, usage) =>
                val ecPoint = key.publicKey.getCurvePoint
                node_models.PublicKey(
                  id = keyId,
                  usage = usage,
                  addedOn = Some(
                    node_models
                      .TimestampInfo(1, 1, earlierTimestamp.toProtoTimestamp.some)
                  ),
                  revokedOn =
                    revokedTimestamp.map(instant => node_models.TimestampInfo(1, 1, instant.toProtoTimestamp.some)),
                  keyData = node_models.PublicKey.KeyData.EcKeyData(
                    node_models.ECKeyData(
                      ECConfig.CURVE_NAME,
                      x = ByteString.copyFrom(ecPoint.x.toByteArray.dropWhile(_ == 0)),
                      y = ByteString.copyFrom(ecPoint.y.toByteArray.dropWhile(_ == 0))
                    )
                  )
                )
            }
          )
        )
      )
      doReturn(Future.successful(response)).when(nodeMock).getDidDocument(GetDidDocumentRequest(issuerDID.value))

      val request = connector_api.GetConnectionCommunicationKeysRequest(connectionId = connectionId.toString)
      val rpcRequest = SignedRpcRequest.generate(holderKey, holderDID, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getConnectionCommunicationKeys(request)

        // TODO remove "master" key when we stop filtering out non-communication keys
        val expectedKeyNames = Set("foo", "bar", "master")
        val expectedKeys = issuerCommKeys.filter(k => expectedKeyNames.contains(k._1)).map {
          case (keyId, key, _, _) =>
            (keyId, key.publicKey.getEncoded.toVector)
        }

        response.keys.map(key =>
          (key.keyId, key.key.get.publicKey.toByteArray.toVector)
        ) must contain theSameElementsAs expectedKeys
      }

      val requestCaptor = ArgCaptor[node_api.GetDidDocumentRequest]
      verify(nodeMock, atLeast(1)).getDidDocument(requestCaptor)
      requestCaptor.value.did mustBe issuerDID.value
    }

    "return connection keys for a participant with key known to connector" in {
      val holderKey = EC.generateKeyPair()
      val issuerAuthKey = EC.generateKeyPair()
      val did = generateDid(issuerAuthKey.publicKey)

      val issuerId = createIssuer("Issuer", publicKey = Some(issuerAuthKey.publicKey), did = Some(did))
      val holderId = createHolder("Holder", publicKey = Some(holderKey.publicKey))
      val connectionId = createConnection(issuerId, holderId)

      val request = connector_api.GetConnectionCommunicationKeysRequest(connectionId = connectionId.toString)
      val rpcRequest = SignedRpcRequest.generate(issuerAuthKey, did, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getConnectionCommunicationKeys(request)
        response.keys.size mustBe 1
        response.keys.head.keyId mustBe ("")
        response.keys.head.key.get.publicKey.toByteArray must contain theSameElementsAs holderKey.publicKey.getEncoded
      }
    }
  }
}
