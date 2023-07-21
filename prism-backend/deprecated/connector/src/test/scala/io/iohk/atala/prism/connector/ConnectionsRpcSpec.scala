package io.iohk.atala.prism.connector

import cats.effect.unsafe.implicits.global
import cats.syntax.option._
import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.connector.model.ParticipantType.Holder
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO, ParticipantsDAO}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.protos.connector_api.GetConnectionTokenInfoRequest
import io.iohk.atala.prism.protos.node_api.GetDidDocumentRequest
import io.iohk.atala.prism.protos.node_models.{KeyUsage, LedgerData}
import io.iohk.atala.prism.protos.{connector_api, connector_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import io.iohk.atala.prism.{DIDUtil, auth}
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import org.scalatest.OptionValues._

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.concurrent.Future
import scala.util.Random

class ConnectionsRpcSpec extends ConnectorRpcSpecBase with MockitoSugar {

  "GenerateConnectionTokens" should {
    "generate connection tokens" in {
      val (keyPair, did) = createDid
      testConnectionTokensGeneration(keyPair, keyPair.getPublicKey, did)
    }

    "generate connection tokens using unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testConnectionTokensGeneration(keyPair, keyPair.getPublicKey, did)
    }

    "generate connection tokens when tokens count is not supplied" in {
      val (keyPair, did) = createDid
      val _ = createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
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
      val (keyPair, did) = createDid
      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val token = createToken(issuerId)
      val request = connector_api.GetConnectionTokenInfoRequest(token.token)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      testGetConnectionToken(rpcRequest, request, did.toString)
    }

    "return token info when user use unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      val issuerId =
        createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
      val token = createToken(issuerId)
      val request = connector_api.GetConnectionTokenInfoRequest(token.token)
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      testGetConnectionToken(rpcRequest, request, did.asCanonical().toString)
    }

    "returns UNKNOWN if token does not exist" in {
      val (keyPair, did) = createDid
      val _ = createIssuer("Issuer", Some(keyPair.getPublicKey), Some(did))
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
      val encodedPubKey = connector_models.EncodedPublicKey(
        ByteString.copyFrom(keys.getPublicKey.getEncoded)
      )
      val request = connector_api
        .AddConnectionFromTokenRequest(token.token)
        .withHolderEncodedPublicKey(encodedPubKey)
      usingApiAs(Vector.empty, keys, request) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(request)
        response.connection.value.participantName mustBe "Issuer"
        val connectionId =
          ConnectionId.unsafeFrom(response.connection.value.connectionId)

        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        val result = ParticipantsDAO
          .findByPublicKey(keys.getPublicKey)
          .transact(database)
          .value
          .unsafeToFuture()
          .futureValue
          .value

        result.publicKey.value must be(keys.getPublicKey)
        result.tpe must be(Holder)
      }
    }

    "add connection from token using unpublished did auth" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val (keyPair, unpublishedDID) = DIDUtil.createUnpublishedDid
      val request = connector_api
        .AddConnectionFromTokenRequest(token.token)
      val rpcRequest =
        SignedRpcRequest.generate(keyPair, unpublishedDID, request)
      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(request)
        val connectionId =
          ConnectionId.unsafeFrom(response.connection.value.connectionId)

        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true

        val result = ParticipantsDAO
          .findByDID(unpublishedDID)
          .transact(database)
          .value
          .unsafeToFuture()
          .futureValue
          .value

        result.publicKey must be(empty)
        result.tpe must be(Holder)
        result.did must be(Option(unpublishedDID.asCanonical()))
      }
    }

    "fail to add connection when signature missing" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(
        ByteString.copyFrom(keys.getPublicKey.getEncoded)
      )
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
      val issuer = createIssuer("Issuer")
      val (holderKeys, holderDID) = DIDUtil.createUnpublishedDid
      val tokenStr1 = createToken(issuer)
      val tokenStr2 = createToken(issuer)
      val encodedHolderPublicKey =
        connector_models.EncodedPublicKey(
          ByteString.copyFrom(holderKeys.getPublicKey.getEncoded)
        )

      val request1 = connector_api.AddConnectionFromTokenRequest(
        tokenStr1.token,
        Some(encodedHolderPublicKey)
      )
      val request2 = connector_api.AddConnectionFromTokenRequest(
        tokenStr2.token,
        Some(encodedHolderPublicKey)
      )

      val signedRequest1 =
        SignedRpcRequest.generate(holderKeys, holderDID, request1)
      val signedRequest2 =
        SignedRpcRequest.generate(holderKeys, holderDID, request2)

      usingApiAs(signedRequest1) { blockingStub =>
        // first connection should be established
        val response = blockingStub.addConnectionFromToken(request1)
        val connectionId =
          ConnectionId.unsafeFrom(response.connection.value.connectionId)
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
        connector_models.EncodedPublicKey(
          ByteString.copyFrom(holderKeys.getPublicKey.getEncoded)
        )

      val request1 = connector_api.AddConnectionFromTokenRequest(
        tokenStr1.token,
        Some(encodedHolderPublicKey)
      )
      val request2 = connector_api.AddConnectionFromTokenRequest(
        tokenStr2.token,
        Some(encodedHolderPublicKey)
      )

      usingApiAs(Vector.empty, holderKeys, request1) { blockingStub =>
        // first connection should be established
        val response = blockingStub.addConnectionFromToken(request1)
        val connectionId =
          ConnectionId.unsafeFrom(response.connection.value.connectionId)
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
        status.getDescription must include(
          holderKeys.getPublicKey.toString.takeWhile(_ != '@')
        )
      }
    }

    "return UNKNOWN if the token does not exist" in {
      val token = TokenString.random()
      val keys = EC.generateKeyPair()
      val encodedPubKey = connector_models.EncodedPublicKey(
        ByteString.copyFrom(keys.getPublicKey.getEncoded)
      )

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
      val encodedPubKey = connector_models.EncodedPublicKey(
        ByteString.copyFrom(keys.getPublicKey.getEncoded)
      )
      val addTokenRequest = connector_api
        .AddConnectionFromTokenRequest(token.token)
        .withHolderEncodedPublicKey(encodedPubKey)

      // create connection
      val connectionId =
        usingApiAs(Random.nextBytes(80).toVector, keys, addTokenRequest) { blockingStub =>
          val response = blockingStub.addConnectionFromToken(addTokenRequest)
          val connectionId =
            ConnectionId.unsafeFrom(response.connection.value.connectionId)
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

    "work with unpublished did auth" in {
      val issuerId = createIssuer("Issuer")
      val token = createToken(issuerId)
      val (keys, unpublishedDID) = DIDUtil.createUnpublishedDid
      val addTokenRequest = connector_api
        .AddConnectionFromTokenRequest(token.token)
      val uhpublishedDidAddTokenRequest =
        SignedRpcRequest.generate(keys, unpublishedDID, addTokenRequest)

      // create connection
      val connectionId = usingApiAs(uhpublishedDidAddTokenRequest) { blockingStub =>
        val response = blockingStub.addConnectionFromToken(addTokenRequest)
        val connectionId =
          ConnectionId.unsafeFrom(response.connection.value.connectionId)
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
      val revokeRequestWithUnpublishedDIdRequest =
        SignedRpcRequest.generate(keys, unpublishedDID, revokeRequest)

      usingApiAs(revokeRequestWithUnpublishedDIdRequest) { blockingStub =>
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
      val (keyPair, did) = createDid
      testNewConnectionsReturnPaginated(keyPair, keyPair.getPublicKey, did)
    }

    "return new connections while authorized by unpublished did" in {
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testNewConnectionsReturnPaginated(keyPair, keyPair.getPublicKey, did)
    }

    "return new connections authenticating by signature" in {
      val (keys, did) = createDid
      val request = connector_api.GetConnectionsPaginatedRequest("", 10)
      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        EC.signBytes(
          auth.model.RequestNonce(requestNonce).mergeWith(request.toByteArray).toArray,
          keys.getPrivateKey
        )

      val verifierId =
        createVerifier("Verifier", Some(keys.getPublicKey), Some(did))

      val zeroTime = System.currentTimeMillis()
      val connections = createExampleConnections(verifierId, zeroTime)

      usingApiAs(
        requestNonce,
        signature,
        did,
        DID.getDEFAULT_MASTER_KEY_ID,
        TraceId.generateYOLO
      ) { blockingStub =>
        val response = blockingStub.getConnectionsPaginated(request)
        response.connections
          .map(_.connectionId)
          .toSet mustBe connections.map(_._2.toString).take(10).toList.toSet
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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
      val (keyPair, did) = createDid
      val _ = createVerifier("Verifier", Some(keyPair.getPublicKey), Some(did))
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
      val earlierTimestamp =
        LocalDateTime.of(2020, 5, 12, 0, 0).toInstant(ZoneOffset.UTC)
      val laterTimestamp =
        LocalDateTime.of(2020, 5, 13, 0, 0).toInstant(ZoneOffset.UTC)
      val issuerKeyAgreementKeys: Seq[
        (String, ECKeyPair, Option[Instant], KeyUsage.KEY_AGREEMENT_KEY.type)
      ] = Seq(
        (
          "foo",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "bar",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "revoked",
          EC.generateKeyPair(),
          Some(laterTimestamp),
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "master",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        )
      )
      val (holderKeys, holderDID) = createDid
      val (issuerKeys, issuerDID) = createDid
      testReturningNonRevokedKeysForADIDOwningParticipant(
        earlierTimestamp,
        issuerKeyAgreementKeys,
        issuerKeys,
        holderKeys,
        issuerDID,
        holderDID
      )
    }

    "return non-revoked keys for a unpublished DID owning participant" in {
      val earlierTimestamp =
        LocalDateTime.of(2020, 5, 12, 0, 0).toInstant(ZoneOffset.UTC)
      val laterTimestamp =
        LocalDateTime.of(2020, 5, 13, 0, 0).toInstant(ZoneOffset.UTC)
      val issuerKeyAgreementKeys: Seq[
        (String, ECKeyPair, Option[Instant], KeyUsage.KEY_AGREEMENT_KEY.type)
      ] = Seq(
        (
          "foo",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "bar",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "revoked",
          EC.generateKeyPair(),
          Some(laterTimestamp),
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        ),
        (
          "master",
          EC.generateKeyPair(),
          None,
          node_models.KeyUsage.KEY_AGREEMENT_KEY
        )
      )

      val (holderKeys, holderDID) = DIDUtil.createUnpublishedDid
      val (issuerKeys, issuerDID) = createDid
      testReturningNonRevokedKeysForADIDOwningParticipant(
        earlierTimestamp,
        issuerKeyAgreementKeys,
        issuerKeys,
        holderKeys,
        issuerDID,
        holderDID
      )
    }

    "return connection keys for a participant with key known to connector" in {
      val holderKey = EC.generateKeyPair()
      val (issuerKeys, issuerDID) = createDid

      val issuerId = createIssuer(
        "Issuer",
        publicKey = Some(issuerKeys.getPublicKey),
        did = Some(issuerDID)
      )
      val holderId =
        createHolder("Holder", publicKey = Some(holderKey.getPublicKey))
      val connectionId = createConnection(issuerId, holderId)

      val request =
        connector_api.GetConnectionCommunicationKeysRequest(connectionId = connectionId.toString)
      val rpcRequest = SignedRpcRequest.generate(issuerKeys, issuerDID, request)

      usingApiAs(rpcRequest) { blockingStub =>
        val response = blockingStub.getConnectionCommunicationKeys(request)
        response.keys.size mustBe 1
        response.keys.head.keyId mustBe ("")
        response.keys.head.key.get.publicKey.toByteArray must contain theSameElementsAs holderKey.getPublicKey.getEncoded
      }
    }
  }

  private def testGetConnectionToken(
      rpcRequest: SignedRpcRequest[GetConnectionTokenInfoRequest],
      request: GetConnectionTokenInfoRequest,
      expectedDid: String
  ): Assertion = {
    usingApiAs(rpcRequest) { blockingStub =>
      val response = blockingStub.getConnectionTokenInfo(request)
      response.creatorName mustBe "Issuer"
      response.creatorLogo.size() must be > 0 // the issuer has a logo
      response.creatorDid mustBe expectedDid
    }
  }

  private def testConnectionTokensGeneration(
      keyPair: ECKeyPair,
      issuerPublicKey: ECPublicKey,
      did: DID
  ): Unit = {
    val tokensCount = 3
    val _ = createIssuer("Issuer", Some(issuerPublicKey), Some(did))
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

  private def testNewConnectionsReturnPaginated(
      keyPair: ECKeyPair,
      publicKey: ECPublicKey,
      did: DID
  ): Assertion = {
    val verifierId = createVerifier("Verifier", Some(publicKey), Some(did))
    val request = connector_api.GetConnectionsPaginatedRequest("", 10)
    val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

    val zeroTime = System.currentTimeMillis()
    val connections = createExampleConnections(verifierId, zeroTime)

    val response = usingApiAs(rpcRequest)(_.getConnectionsPaginated(request))

    response.connections
      .map(_.connectionId)
      .toSet mustBe connections.map(_._2.toString).take(10).toList.toSet

    val nextRequest = connector_api.GetConnectionsPaginatedRequest(
      response.connections.last.connectionId,
      10
    )
    val nextRpcRequest = SignedRpcRequest.generate(keyPair, did, nextRequest)

    usingApiAs(nextRpcRequest) { blockingStub =>
      val nextResponse = blockingStub.getConnectionsPaginated(nextRequest)
      nextResponse.connections
        .map(_.connectionId)
        .toSet mustBe connections.map(_._2.toString).slice(10, 20).toList.toSet
    }
  }

  private def testReturningNonRevokedKeysForADIDOwningParticipant(
      earlierTimestamp: Instant,
      issuerKeyAgreementKeys: Seq[
        (String, ECKeyPair, Option[Instant], KeyUsage.KEY_AGREEMENT_KEY.type)
      ],
      issuerAuthKey: ECKeyPair,
      holderKey: ECKeyPair,
      issuerDID: DID,
      holderDID: DID
  ) = {
    val issuerId =
      createIssuer(
        "Issuer",
        publicKey = Some(issuerAuthKey.getPublicKey),
        did = Some(issuerDID)
      )
    val holderId = createHolder(
      "Holder",
      publicKey = Some(holderKey.getPublicKey),
      did = Some(holderDID)
    )
    val connectionId = createConnection(issuerId, holderId)
    val response = node_api.GetDidDocumentResponse(
      Some(
        node_models.DIDData(
          id = issuerDID.getSuffix,
          publicKeys = issuerKeyAgreementKeys.map { case (keyId, key, revokedTimestamp, usage) =>
            val ecPoint = key.getPublicKey.getCurvePoint
            node_models.PublicKey(
              id = keyId,
              usage = usage,
              addedOn = Some(
                LedgerData(timestampInfo =
                  Some(
                    node_models
                      .TimestampInfo(
                        1,
                        1,
                        earlierTimestamp.toProtoTimestamp.some
                      )
                  )
                )
              ),
              revokedOn = revokedTimestamp.map(instant =>
                LedgerData(timestampInfo =
                  Some(
                    node_models
                      .TimestampInfo(1, 1, instant.toProtoTimestamp.some)
                  )
                )
              ),
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                node_models.ECKeyData(
                  ECConfig.getCURVE_NAME,
                  x = ByteString.copyFrom(
                    ecPoint.getX.bytes().dropWhile(_ == 0)
                  ),
                  y = ByteString.copyFrom(
                    ecPoint.getY.bytes().dropWhile(_ == 0)
                  )
                )
              )
            )
          }
        )
      )
    )
    doReturn(Future.successful(response))
      .when(nodeMock)
      .getDidDocument(GetDidDocumentRequest(issuerDID.getValue))

    val request =
      connector_api.GetConnectionCommunicationKeysRequest(connectionId = connectionId.toString)
    val rpcRequest = SignedRpcRequest.generate(holderKey, holderDID, request)

    usingApiAs(rpcRequest) { blockingStub =>
      val response = blockingStub.getConnectionCommunicationKeys(request)

      // TODO remove "master" key when we stop filtering out non-communication keys
      val expectedKeyNames = Set("foo", "bar", "master")
      val expectedKeys =
        issuerKeyAgreementKeys.filter(k => expectedKeyNames.contains(k._1)).map { case (keyId, key, _, _) =>
          (keyId, key.getPublicKey.getEncoded.toVector)
        }

      response.keys.map(key =>
        (key.keyId, key.key.get.publicKey.toByteArray.toVector)
      ) must contain theSameElementsAs expectedKeys
    }

    val requestCaptor = ArgCaptor[node_api.GetDidDocumentRequest]
    verify(nodeMock, atLeast(1)).getDidDocument(requestCaptor)
    requestCaptor.value.did mustBe issuerDID.getValue
  }
}
