package io.iohk.connector

import java.util.UUID

import com.google.protobuf.ByteString
import doobie.implicits._
import io.grpc.{Status, StatusRuntimeException}
import io.iohk.connector.model.RequestNonce
import io.iohk.connector.repositories.daos.MessagesDAO
import io.iohk.cvp.crypto.ECKeys.toEncodedPublicKey
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.grpc.SignedRequestsHelper
import io.iohk.prism.protos.connector_api

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
      val keys = ECKeys.generateKeyPair()
      val privateKey = keys.getPrivate
      val encodedPublicKey = toEncodedPublicKey(keys.getPublic)
      val request = connector_api.GetMessagesPaginatedRequest("", 10)

      val requestNonce = UUID.randomUUID().toString.getBytes.toVector
      val signature =
        ECSignature.sign(
          privateKey,
          SignedRequestsHelper.merge(RequestNonce(requestNonce), request.toByteArray).toArray
        )
      val issuerId = createIssuer("Issuer", Some(encodedPublicKey))

      val messages = createExampleMessages(issuerId)

      usingApiAs(requestNonce, signature, encodedPublicKey) { blockingStub =>
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
