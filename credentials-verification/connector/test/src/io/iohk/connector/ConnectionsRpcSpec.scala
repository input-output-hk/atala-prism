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
  }
}
