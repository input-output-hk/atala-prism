package io.iohk.atala.prism.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.protos.connector_api.RegisterDIDRequest
import io.iohk.atala.prism.protos.node_api.CreateDIDResponse
import io.iohk.atala.prism.protos.{common_models, connector_api, node_models}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.util.Try

class RegistrationRpcSpec extends ConnectorRpcSpecBase {

  def createRequest(name: String, logo: Array[Byte]): RegisterDIDRequest = {
    connector_api
      .RegisterDIDRequest(name = name)
      .withLogo(ByteString.copyFrom(logo))
      .withRole(connector_api.RegisterDIDRequest.Role.issuer)
      .withCreateDIDOperation(node_models.SignedAtalaOperation())
  }

  "registerDID" should {
    "work" in {
      usingApiAs.unlogged { blockingStub =>
        val expectedDID = DataPreparation.newDID()
        val name = "iohk"
        val logo = "none".getBytes()
        val transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value
        val request = createRequest(name, logo)

        nodeMock.createDID(*).returns {
          Future.successful(
            CreateDIDResponse(expectedDID.suffix.value).withTransactionInfo(
              common_models
                .TransactionInfo()
                .withTransactionId(transactionId.toString)
                .withLedger(common_models.Ledger.IN_MEMORY)
            )
          )
        }
        val response = blockingStub.registerDID(request)

        // returns the did
        response.did must be(expectedDID.value)

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

    "fail when the user tries to register the same DID twice" in {
      val expectedDID = DataPreparation.newDID()
      val name = "iohk"
      val logo = "none".getBytes()
      val transactionId = TransactionId.from(SHA256Digest.compute("id".getBytes).value).value
      val request = createRequest(name, logo)

      usingApiAs.unlogged { blockingStub =>
        nodeMock.createDID(*).returns {
          Future.successful(
            CreateDIDResponse(expectedDID.suffix.value).withTransactionInfo(
              common_models
                .TransactionInfo()
                .withTransactionId(transactionId.toString)
                .withLedger(common_models.Ledger.IN_MEMORY)
            )
          )
        }
        val response1 = blockingStub.registerDID(request)
        val response2 = Try(blockingStub.registerDID(request))
        response1.did must be(expectedDID.value)
        response2.toEither.left.map(_.getMessage) mustBe Left("INVALID_ARGUMENT: DID already exists")
      }
    }
  }
}
