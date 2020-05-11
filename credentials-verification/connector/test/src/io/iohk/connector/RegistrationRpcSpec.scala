package io.iohk.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.IssuersRepository
import io.iohk.cvp.cstore.models.Verifier
import io.iohk.cvp.cstore.repositories.VerifiersRepository
import io.iohk.cvp.models.ParticipantId
import io.iohk.prism.protos.node_api.CreateDIDResponse
import io.iohk.prism.protos.{connector_api, node_models}
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
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withCreateDIDOperation(node_models.SignedAtalaOperation())

        nodeMock.createDID(*).returns {
          Future.successful(CreateDIDResponse("test"))
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
      }
    }

    "propagate the participant to the issuers table" in {
      val name = "Blockchain University"
      val participantId =
        register(didSuffix = "issuerX", name = name, role = connector_api.RegisterDIDRequest.Role.issuer)
      val result = new IssuersRepository(database).findBy(Issuer.Id(participantId.uuid)).value.futureValue
      result map (_.value.id.value) must be(Right(participantId.uuid))
    }

    "propagate the participant to the verifiers table" in {
      val name = "Blockchain Employer"
      val participantId =
        register(didSuffix = "employerX", name = name, role = connector_api.RegisterDIDRequest.Role.verifier)
      val result = new VerifiersRepository(database).findBy(Verifier.Id(participantId.uuid)).value.futureValue
      result map (_.value.id.value) must be(Right(participantId.uuid))
    }
  }

  private def register(
      didSuffix: String,
      name: String,
      role: connector_api.RegisterDIDRequest.Role
  ): ParticipantId = {
    usingApiAs.unlogged { blockingStub =>
      val expectedDID = s"did:prism:$didSuffix"
      val logo = "none".getBytes()
      val request = connector_api
        .RegisterDIDRequest(name = name)
        .withLogo(ByteString.copyFrom(logo))
        .withRole(role)
        .withCreateDIDOperation(node_models.SignedAtalaOperation())

      nodeMock.createDID(*).returns {
        Future.successful(CreateDIDResponse(didSuffix))
      }
      blockingStub.registerDID(request)

      val id = ParticipantsDAO
        .findByDID(expectedDID)
        .transact(database)
        .value
        .unsafeRunSync()
        .value
        .id

      id
    }
  }
}
