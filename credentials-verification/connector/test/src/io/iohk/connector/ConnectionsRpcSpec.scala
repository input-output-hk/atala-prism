package io.iohk.connector

import java.util.UUID

import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.connector.model._
import io.iohk.connector.protos.{
  AddConnectionFromTokenRequest,
  GenerateConnectionTokenRequest,
  GetConnectionTokenInfoRequest,
  GetConnectionsPaginatedRequest
}
import io.iohk.connector.repositories.daos.{ConnectionTokensDAO, ConnectionsDAO}

class ConnectionsRpcSpec extends RpcSpecBase {

  "GenerateConnectionToken" should {
    "generate connection token" in {
      val issuerId = createIssuer("Issuer")

      usingApiAs(issuerId) { blockingStub =>
        val request = GenerateConnectionTokenRequest()

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
        val request = GetConnectionTokenInfoRequest(token.token)
        val response = blockingStub.getConnectionTokenInfo(request)
        response.creator.getIssuer.name mustBe "Issuer"
      }
    }

    "returns UNKNOWN if token does not exist" in {
      val issuerId = createIssuer("Issuer")

      usingApiAs(issuerId) { blockingStub =>
        val token = TokenString.random()

        val request = GetConnectionTokenInfoRequest(token.token)
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
      val holderId = createHolder("Holder")
      val token = createToken(issuerId)

      usingApiAs(holderId) { blockingStub =>
        val request = AddConnectionFromTokenRequest(token.token)
        val response = blockingStub.addConnectionFromToken(request)
        response.connection.participantInfo.getIssuer.name mustBe "Issuer"
        val connectionId = new ConnectionId(UUID.fromString(response.connection.connectionId))

        ConnectionsDAO
          .exists(connectionId)
          .transact(database)
          .unsafeToFuture()
          .futureValue mustBe true
      }
    }

    "return UNKNOWN if the token does not exist" in {
      val holderId = createHolder("Holder")
      val token = TokenString.random()

      usingApiAs(holderId) { blockingStub =>
        val request = AddConnectionFromTokenRequest(token.token)

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
        val request = GetConnectionsPaginatedRequest("", 10)
        val response = blockingStub.getConnectionsPaginated(request)
        response.connections.map(_.connectionId).toSet mustBe connections.map(_._2.id.toString).take(10).toList.toSet

        val nextRequest = GetConnectionsPaginatedRequest(response.connections.last.connectionId, 10)
        val nextResponse = blockingStub.getConnectionsPaginated(nextRequest)
        nextResponse.connections
          .map(_.connectionId)
          .toSet mustBe connections.map(_._2.id.toString).slice(10, 20).toList.toSet
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = GetConnectionsPaginatedRequest("", 0)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when limit is negative" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = GetConnectionsPaginatedRequest("", -7)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when provided id is not a valid" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = GetConnectionsPaginatedRequest("uaoen", 10)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getConnectionsPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
        status.getDescription must include("uaoen")
      }
    }
  }
}
