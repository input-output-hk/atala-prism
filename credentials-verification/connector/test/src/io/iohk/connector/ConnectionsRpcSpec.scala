package io.iohk.connector

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.connector.model.ParticipantType.Holder
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.cvp.crypto.ECKeys._
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.SignedRequestsHelper
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.{connector_api, connector_models, node_api, node_models}
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._

import scala.concurrent.Future

class ConnectionsRpcSpec extends ConnectorRpcSpecBase with MockitoSugar {

  "GenerateConnectionToken" should {
    "generate connection token" in {
      val issuerId = createIssuer("Issuer")

      usingApiAs(issuerId) { blockingStub =>
        val request = connector_api.GenerateConnectionTokenRequest()

        val response = blockingStub.generateConnectionToken(request)
        val token = new TokenString(response.token)

        ConnectionTokensDAO
          .exists(token)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true
      }
    }
  }

  "GetConnectionTokenInfo" should {
    "return token info" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)

      usingApiAs(issuerId) { blockingStub =>
        val request = connector_api.GetConnectionTokenInfoRequest(token.token)
        val response = blockingStub.getConnectionTokenInfo(request)
        response.creator.value.getIssuer.name mustBe "Issuer"
        response.creator.value.getIssuer.logo.size() must be > 0 // the issuer has a logo
      }
    }

    "returns UNKNOWN if token does not exist" in {
      val issuerId = createIssuer("Issuer")

      usingApiAs(issuerId) { blockingStub =>
        val token = TokenString.random()

        val request = connector_api.GetConnectionTokenInfoRequest(token.token)
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
      val keys = generateKeyPair()
      val ecPoint = getECPoint(keys.getPublic)
      val publicKeyProto =
        connector_models.ConnectorPublicKey(ecPoint.getAffineX.toString(), ecPoint.getAffineY.toString())
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      val request = connector_api.AddConnectionFromTokenRequest(token.token).withHolderPublicKey(publicKeyProto)
      usingApiAs(Vector.empty, keys, request) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(request)
        val holderId = response.userId
        holderId mustNot be(empty)
        response.connection.value.participantInfo.value.getIssuer.name mustBe "Issuer"
        val connectionId = new ConnectionId(UUID.fromString(response.connection.value.connectionId))

        val participantInfo = io.iohk.connector.model.ParticipantInfo(
          ParticipantId(holderId),
          Holder,
          Some(encodedPublicKey),
          "",
          None,
          None
        )
        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        ParticipantsDAO
          .findByPublicKey(encodedPublicKey)
          .transact(database)
          .value
          .unsafeToFuture()
          .futureValue mustBe Some(participantInfo)
      }
    }

    "fails to add connection when signature missing" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = generateKeyPair()
      val ecPoint = getECPoint(keys.getPublic)
      val publicKeyProto =
        connector_models.ConnectorPublicKey(ecPoint.getAffineX.toString(), ecPoint.getAffineY.toString())
      usingApiAs.unlogged { blockingStub =>
        val request = connector_api.AddConnectionFromTokenRequest(token.token).withHolderPublicKey(publicKeyProto)
        val ex = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request)
        }
        ex.getStatus.getCode mustBe Status.Code.UNAUTHENTICATED
      }
    }

    "return UNKNOWN if the token does not exist" in {
      val token = TokenString.random()
      val keys = generateKeyPair()
      val ecPoint = getECPoint(keys.getPublic)
      val publicKeyProto =
        connector_models.ConnectorPublicKey(ecPoint.getAffineX.toString(), ecPoint.getAffineY.toString())

      val request = connector_api.AddConnectionFromTokenRequest(token.token).withHolderPublicKey(publicKeyProto)
      usingApiAs(Vector.empty, keys, request) { blockingStub =>
        val status = intercept[StatusRuntimeException] {
          blockingStub.addConnectionFromToken(request)
        }.getStatus

        status.getCode mustBe Status.Code.UNKNOWN
        status.getDescription must include(token.token)
      }
    }
  }

  "GetConnectionsPaginated" should {
    "return new connections" in {
      val verifierId = createVerifier("Verifier")

      val zeroTime = System.currentTimeMillis()
      val connections = createExampleConnections(verifierId, zeroTime)

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetConnectionsPaginatedRequest("", 10)
        val response = blockingStub.getConnectionsPaginated(request)
        response.connections.map(_.connectionId).toSet mustBe connections.map(_._2.id.toString).take(10).toList.toSet

        val nextRequest = connector_api.GetConnectionsPaginatedRequest(response.connections.last.connectionId, 10)
        val nextResponse = blockingStub.getConnectionsPaginated(nextRequest)
        nextResponse.connections
          .map(_.connectionId)
          .toSet mustBe connections.map(_._2.id.toString).slice(10, 20).toList.toSet
      }
    }

    "return new connections authenticating by signature" in {
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      val request = connector_api.GetConnectionsPaginatedRequest("", 10)
      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        ECSignature.sign(
          privateKey,
          SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
        )

      val verifierId = createVerifier("Verifier", Some(encodedPublicKey))

      val zeroTime = System.currentTimeMillis()
      val connections = createExampleConnections(verifierId, zeroTime)

      usingApiAs(requestNonce, signature, encodedPublicKey) { blockingStub =>
        val response = blockingStub.getConnectionsPaginated(request)
        response.connections.map(_.connectionId).toSet mustBe connections.map(_._2.id.toString).take(10).toList.toSet
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetConnectionsPaginatedRequest("", 0)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when limit is negative" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetConnectionsPaginatedRequest("", -7)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when provided id is not a valid" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetConnectionsPaginatedRequest("uaoen", 10)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
        status.getDescription must include("uaoen")
      }
    }

    "return non-revoked keys for a DID owning participant" in {
      val earlierTimestamp = LocalDateTime.of(2020, 5, 12, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000L
      val laterTimestamp = LocalDateTime.of(2020, 5, 13, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000L

      val holderKey = generateKeyPair()
      val encodedHolderKey = ECKeys.toEncodedPublicKey(holderKey.getPublic)
      val issuerAuthKey = generateKeyPair()
      val encodedIssuerAuthKey = ECKeys.toEncodedPublicKey(issuerAuthKey.getPublic)

      val issuerCommKeys = Seq(
        ("foo", generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY),
        ("bar", generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY),
        ("revoked", generateKeyPair(), Some(laterTimestamp), node_models.KeyUsage.COMMUNICATION_KEY),
        ("master", generateKeyPair(), None, node_models.KeyUsage.COMMUNICATION_KEY)
      )

      val issuerId = createIssuer("Issuer", publicKey = Some(encodedIssuerAuthKey), did = Some("did:prism:issuer"))
      val holderId = createHolder("Holder", publicKey = Some(encodedHolderKey))
      val connectionId = createConnection(issuerId, holderId)

      val response = node_api.GetDidDocumentResponse(
        Some(
          node_models.DIDData(
            id = "issuer",
            publicKeys = issuerCommKeys.map {
              case (keyId, key, revokedTimestamp, usage) =>
                val ecPoint = ECKeys.getECPoint(key.getPublic)
                node_models.PublicKey(
                  id = keyId,
                  usage = usage,
                  addedOn = Some(node_models.TimestampInfo(earlierTimestamp, 1, 1)),
                  revokedOn = revokedTimestamp.map(node_models.TimestampInfo(_, 1, 1)),
                  keyData = node_models.PublicKey.KeyData.EcKeyData(
                    node_models.ECKeyData(
                      ECKeys.CURVE_NAME,
                      x = ByteString.copyFrom(ecPoint.getAffineX.toByteArray.dropWhile(_ == 0)),
                      y = ByteString.copyFrom(ecPoint.getAffineY.toByteArray.dropWhile(_ == 0))
                    )
                  )
                )
            }.toSeq
          )
        )
      )
      doReturn(Future.successful(response)).when(nodeMock).getDidDocument(*)

      usingApiAs(holderId) { blockingStub =>
        val request = connector_api.GetConnectionCommunicationKeysRequest(
          connectionId = connectionId.id.toString
        )
        val response = blockingStub.getConnectionCommunicationKeys(request)

        // TODO remove "master" key when we stop filtering out non-communication keys
        val expectedKeyNames = Set("foo", "bar", "master")
        val expectedKeys = issuerCommKeys.filter(k => expectedKeyNames.contains(k._1)).map {
          case (keyId, key, _, _) =>
            (keyId, ECKeys.toEncodedPublicKey(key.getPublic))
        }

        response.keys.map(key =>
          (key.keyId, EncodedPublicKey(key.key.get.publicKey.toByteArray.toVector))
        ) must contain theSameElementsAs expectedKeys
      }

      val requestCaptor = ArgCaptor[node_api.GetDidDocumentRequest]
      verify(nodeMock).getDidDocument(requestCaptor)
      requestCaptor.value.did mustBe "did:prism:issuer"
    }

    "return connection keys for a participant with key known to connector" in {
      val holderKey = generateKeyPair()
      val encodedHolderKey = ECKeys.toEncodedPublicKey(holderKey.getPublic)
      val issuerAuthKey = generateKeyPair()
      val encodedIssuerAuthKey = ECKeys.toEncodedPublicKey(issuerAuthKey.getPublic)

      val issuerId = createIssuer("Issuer", publicKey = Some(encodedIssuerAuthKey), did = Some("did:prism:issuer"))
      val holderId = createHolder("Holder", publicKey = Some(encodedHolderKey))
      val connectionId = createConnection(issuerId, holderId)

      usingApiAs(issuerId) { blockingStub =>
        val request = connector_api.GetConnectionCommunicationKeysRequest(
          connectionId = connectionId.id.toString
        )

        val response = blockingStub.getConnectionCommunicationKeys(request)
        response.keys.size mustBe 1
        response.keys.head.keyId mustBe ("")
        response.keys.head.key.get.publicKey.toByteArray must contain theSameElementsAs encodedHolderKey.bytes
      }
    }
  }
}
