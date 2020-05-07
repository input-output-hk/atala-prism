package io.iohk.connector

import com.google.protobuf.ByteString
import doobie.implicits._
import io.iohk.connector.model._
import io.iohk.connector.repositories.daos.ParticipantsDAO
import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.repositories.daos.IssuersDAO
import io.iohk.cvp.cstore.repositories.daos.StoreUsersDAO
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
      val (did, participantId) =
        register(didSuffix = "issuerX", name = name, role = connector_api.RegisterDIDRequest.Role.issuer)
      val result = IssuersDAO.findBy(Issuer.Id(participantId.uuid)).transact(database).unsafeRunSync().value
      result.id.value must be(participantId.uuid)
      result.name.value must be(name)
      result.did must be(did)
    }

    "propagate the participant to the verifiers table" in {
      val name = "Blockchain Employer"
      val (did, participantId) =
        register(didSuffix = "employerX", name = name, role = connector_api.RegisterDIDRequest.Role.verifier)
      val result = StoreUsersDAO.get(participantId).transact(database).unsafeRunSync().value
      result.id must be(participantId)
    }
  }

  private def register(
      didSuffix: String,
      name: String,
      role: connector_api.RegisterDIDRequest.Role
  ): (String, ParticipantId) = {
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
      val response = blockingStub.registerDID(request)

      val id = ParticipantsDAO
        .findByDID(expectedDID)
        .transact(database)
        .value
        .unsafeRunSync()
        .value
        .id

      (response.did, id)
    }
  }
}
