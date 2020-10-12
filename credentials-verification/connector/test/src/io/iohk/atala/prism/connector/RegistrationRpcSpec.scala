package io.iohk.atala.prism.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.prism.protos.node_api.CreateDIDResponse
import io.iohk.prism.protos.{common_models, connector_api, node_models}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import scala.concurrent.Future

class RegistrationRpcSpec extends ConnectorRpcSpecBase {

  "registerDID" should {
    "work" in {
      usingApiAs.unlogged { blockingStub =>
        val expectedDID = "did:prism:test"
        val name = "iohk"
        val logo = "none".getBytes()
        val transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withCreateDIDOperation(node_models.SignedAtalaOperation())

        nodeMock.createDID(*).returns {
          Future.successful(
            CreateDIDResponse("test").withTransactionInfo(
              common_models
                .TransactionInfo()
                .withTransactionId(transactionId.toString)
                .withLedger(common_models.Ledger.IN_MEMORY)
            )
          )
        }
        val response = blockingStub.registerDID(request)

        // returns the did
        response.did must be(expectedDID)

        // the participant gets stored
        val result = ParticipantsDAO
          .findByDID(expectedDID)
          .transact(database)
          .value
          .unsafeRunSync()
        result.isDefined must be(true)

        // with the proper values
        val participant = result.value
        participant.logo.value.bytes must be(logo.toVector)
        participant.name must be(name)
        participant.tpe must be(ParticipantType.Issuer)
        participant.transactionId.value must equal(transactionId)
        participant.ledger.value must be(Ledger.InMemory)
      }
    }
  }
}
