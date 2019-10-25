package io.iohk.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.connector.protos.{GetMessagesPaginatedRequest, SendMessageRequest}
import io.iohk.connector.repositories.daos.MessagesDAO

class MessagesRpcSpec extends RpcSpecBase {
  "SendMessage" should {
    "insert message into database" in {
      val issuerId = createIssuer("Issuer")
      val holderId = createHolder("Holder")
      val connectionId = createConnection(issuerId, holderId)

      usingApiAs(issuerId) { blockingStub =>
        val request = SendMessageRequest(connectionId.id.toString, ByteString.copyFrom("test".getBytes))
        val response = blockingStub.sendMessage(request)
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
        val request = GetMessagesPaginatedRequest("", 10)
        val response = blockingStub.getMessagesPaginated(request)
        response.messages.map(m => (m.id, m.connectionId)) mustBe
          messages.take(10).map { case (messageId, connectionId) => (messageId.id.toString, connectionId.id.toString) }
      }
    }
  }

}
