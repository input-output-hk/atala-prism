package io.iohk.atala.prism.connector

import com.google.protobuf.ByteString
import cats.syntax.option._
import doobie.implicits._
import io.iohk.atala.prism.connector.model._
import io.iohk.atala.prism.connector.repositories.daos.ParticipantsDAO
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.atala.prism.protos.connector_api.{RegisterDIDRequest, RegisterDIDResponse}
import io.iohk.atala.prism.protos.node_api.{CreateDIDResponse, GetDidDocumentResponse}
import io.iohk.atala.prism.protos.node_models.DIDData
import io.iohk.atala.prism.protos.{connector_api, node_models}
import io.iohk.atala.prism.util.KeyUtils.createNodePublicKey
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito._
import org.scalatest.Assertion
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.util.Try

class RegistrationRpcSpec extends ConnectorRpcSpecBase {

  def createRequest(name: String, logo: Array[Byte]): RegisterDIDRequest = {
    connector_api
      .RegisterDIDRequest(name = name)
      .withLogo(ByteString.copyFrom(logo))
      .withRole(connector_api.RegisterDIDRequest.Role.issuer)
      .withCreateDidOperation(node_models.SignedAtalaOperation())
  }

  "registerDID" should {
    "work" in {
      usingApiAs.unlogged { blockingStub =>
        val expectedDID = DataPreparation.newDID()
        val name = "iohk"
        val logo = "none".getBytes()
        val operationId = AtalaOperationId.random()
        val request = createRequest(name, logo)

        nodeMock.createDID(*).returns {
          Future.successful(
            CreateDIDResponse(expectedDID.getSuffix)
              .withOperationId(operationId.toProtoByteString)
          )
        }
        val response = blockingStub.registerDID(request)
        checkParticipantProperlyStored(
          response,
          expectedDID,
          name,
          logo,
          operationId.some
        )
      }
    }

    "work then user uses existing did" in {
      usingApiAs.unlogged { blockingStub =>
        val keyId = "key-1"
        val didKeyPair = EC.generateKeyPair()
        val did: DID = DID
          .buildLongFormFromMasterPublicKey(didKeyPair.getPublicKey)
          .asCanonical
        val name = "iohk"
        val logo = "none".getBytes()
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withExistingDid(did.getValue)

        nodeMock.getDidDocument(*).returns {
          Future.successful(
            GetDidDocumentResponse(
              DIDData(
                id = did.getSuffix,
                List(createNodePublicKey(keyId, didKeyPair.getPublicKey))
              ).some
            )
          )
        }
        val response = blockingStub.registerDID(request)
        checkParticipantProperlyStored(response, did, name, logo, None)
      }
    }

    "fail when participant with passed did already exists" in {
      usingApiAs.unlogged { blockingStub =>
        val keyId = "key-1"
        val didKeyPair = EC.generateKeyPair()
        val did: DID = DID
          .buildCanonicalFromMasterPublicKey(didKeyPair.getPublicKey)
          .asCanonical()
        val name = "iohk"
        val participantId = ParticipantId.random()
        val participantRole = ParticipantType.Issuer
        val existingParticipantInfo =
          ParticipantInfo(
            participantId,
            participantRole,
            didKeyPair.getPublicKey.some,
            name,
            did.some,
            None,
            None
          )
        ParticipantsDAO
          .insert(existingParticipantInfo)
          .transact(database)
          .unsafeRunSync()

        nodeMock.getDidDocument(*).returns {
          Future.successful(
            GetDidDocumentResponse(
              DIDData(
                id = did.getSuffix,
                List(createNodePublicKey(keyId, didKeyPair.getPublicKey))
              ).some
            )
          )
        }

        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withExistingDid(did.getValue)

        val response = Try(blockingStub.registerDID(request))
        response.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: DID already exists"
        )
      }
    }

    "fail when the user passed unpublished did" in {
      usingApiAs.unlogged { blockingStub =>
        val didKeyPair = EC.generateKeyPair()
        val did = DID.buildLongFormFromMasterPublicKey(didKeyPair.getPublicKey)
        val name = "iohk"
        val logo = "none".getBytes()
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withExistingDid(did.getValue)

        val response = Try(blockingStub.registerDID(request))
        response.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: Expected published did"
        )
        checkParticipantNotAdded(did)
      }
    }

    "fail when user passed did which can't be found on the node" in {
      usingApiAs.unlogged { blockingStub =>
        val didKeyPair = EC.generateKeyPair()
        val did: DID = DID
          .buildLongFormFromMasterPublicKey(didKeyPair.getPublicKey)
          .asCanonical()
        val name = "iohk"
        val logo = "none".getBytes()
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withExistingDid(did.getValue)

        nodeMock
          .getDidDocument(*)
          .returns(Future.successful(GetDidDocumentResponse()))

        val response = Try(blockingStub.registerDID(request))
        response.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: the passed DID was not found on the node"
        )
        checkParticipantNotAdded(did)
      }
    }

    "fail when user passed non-prism did" in {
      usingApiAs.unlogged { blockingStub =>
        val didKeyPair = EC.generateKeyPair()
        val did: DID = DID
          .buildLongFormFromMasterPublicKey(didKeyPair.getPublicKey)
          .asCanonical()
        val strangeDid = s"did:blabla:${did.getSuffix}"
        val name = "iohk"
        val logo = "none".getBytes()
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)
          .withExistingDid(strangeDid)

        val response = Try(blockingStub.registerDID(request))
        response.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: Invalid DID"
        )
        checkParticipantNotAdded(did)
      }
    }

    "fail when register_with is empty" in {
      usingApiAs.unlogged { blockingStub =>
        val name = "iohk"
        val logo = "none".getBytes()
        val request = connector_api
          .RegisterDIDRequest(name = name)
          .withLogo(ByteString.copyFrom(logo))
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)

        val response = Try(blockingStub.registerDID(request))
        response.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: Expected existing DID or atala operation"
        )
      }
    }

    "fail when the user tries to register the same DID twice" in {
      val expectedDID = DataPreparation.newDID()
      val name = "iohk"
      val logo = "none".getBytes()
      val operationId = Sha256.compute("id".getBytes).getValue
      val request = createRequest(name, logo)

      usingApiAs.unlogged { blockingStub =>
        nodeMock.createDID(*).returns {
          Future.successful(
            CreateDIDResponse(expectedDID.getSuffix)
              .withOperationId(ByteString.copyFrom(operationId))
          )
        }
        val response1 = blockingStub.registerDID(request)
        val response2 = Try(blockingStub.registerDID(request))
        response1.did must be(expectedDID.getValue)
        response2.toEither.left.map(_.getMessage) mustBe Left(
          "INVALID_ARGUMENT: DID already exists"
        )
      }
    }
  }

  // to ensure proper storing of participant
  private def checkParticipantProperlyStored(
      response: RegisterDIDResponse,
      expectedDid: DID,
      expectedName: String,
      logo: Array[Byte],
      expectedOperationId: Option[AtalaOperationId]
  ): Assertion = {
    // returns the did
    response.did mustBe expectedDid.getValue

    // the participant gets stored
    val result = ParticipantsDAO
      .findByDID(expectedDid)
      .transact(database)
      .value
      .unsafeRunSync()
    result.isDefined must be(true)

    // with the proper values
    val participant = result.value
    participant.logo.value.bytes mustBe logo.toVector
    participant.name mustBe expectedName
    participant.tpe mustBe ParticipantType.Issuer
    participant.operationId mustBe expectedOperationId
  }

  // to ensure that service didn't add participant
  private def checkParticipantNotAdded(did: DID): Assertion = {
    val result = ParticipantsDAO
      .findByDID(did)
      .transact(database)
      .value
      .unsafeRunSync()
    result.isEmpty mustBe true
  }
}
