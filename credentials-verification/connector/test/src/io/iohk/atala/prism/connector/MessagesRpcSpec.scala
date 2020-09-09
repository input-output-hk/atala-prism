package io.iohk.atala.prism.connector

import java.util.UUID

import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.atala.crypto.EC
import io.iohk.atala.prism.connector.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.daos.MessagesDAO
import io.iohk.atala.prism.grpc.SignedRequestsHelper
import io.iohk.atala.prism.protos.connector_api

class MessagesRpcSpec extends ConnectorRpcSpecBase {
  "SendMessage" should {
    "insert message into database" in {
      val issuerId = createIssuer("Issuer")
      val holderId = createHolder("Holder")
      val connectionId = createConnection(issuerId, holderId)

      usingApiAs(issuerId) { blockingStub =>
        val request = connector_api.SendMessageRequest(connectionId.id.toString, ByteString.copyFrom("test".getBytes))
        blockingStub.sendMessage(request)
        val msg =
          MessagesDAO.getMessagesPaginated(holderId, 1, None).transact(database).unsafeToFuture().futureValue.head
        msg.connection mustBe connectionId
      }
    }
  }

  "GetMessagesPaginated" should {

    "return messages" in {
      val verifierId = createVerifier("Issuer")
      val messages = createExampleMessages(verifierId)

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetMessagesPaginatedRequest("", 10)
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) => (messageId.id.toString, connectionId.id.toString) }
      }
    }

    "return messages  authenticating by signature" in {
      val keys = EC.generateKeyPair()
      val privateKey = keys.privateKey
      val request = connector_api.GetMessagesPaginatedRequest("", 10)

      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        EC.sign(
          SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray,
          privateKey
        )
      val issuerId = createIssuer("Issuer", Some(keys.publicKey))

      val messages = createExampleMessages(issuerId)

      usingApiAs(requestNonce, signature, keys.publicKey) { blockingStub =>
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) => (messageId.id.toString, connectionId.id.toString) }
      }
    }

    "return INVALID_ARGUMENT when limit is 0" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetMessagesPaginatedRequest("", 0)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when limit is negative" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetMessagesPaginatedRequest("", -7)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
      }
    }

    "return INVALID_ARGUMENT when provided id is not a valid" in {
      val verifierId = createVerifier("Verifier")

      usingApiAs(verifierId) { blockingStub =>
        val request = connector_api.GetMessagesPaginatedRequest("aaa", 10)

        val status = intercept[StatusRuntimeException] {
          blockingStub.getMessagesPaginated(request)
        }.getStatus
        status.getCode mustBe Status.Code.INVALID_ARGUMENT
        status.getDescription must include("aaa")
      }
    }
  }

}
