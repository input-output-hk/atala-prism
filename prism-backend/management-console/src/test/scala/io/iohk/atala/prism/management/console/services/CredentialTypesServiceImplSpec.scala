package io.iohk.atala.prism.management.console.services

import java.util.UUID
import io.grpc.StatusRuntimeException
import org.scalatest.OptionValues
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.protos.{console_api, console_models}
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.models.{
  CreateCredentialTypeField,
  CredentialTypeFieldType,
  CredentialTypeState
}

class CredentialTypesServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil with OptionValues {

  "CredentialTypesServiceImpl" should {

    "return credential types" in new Fixtures {
      val request = console_api.GetCredentialTypesRequest()

      // test empty db
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        stub.getCredentialTypes(request) mustBe console_api
          .GetCredentialTypesResponse(credentialTypes = Nil)
      }

      // create credential type
      credentialTypeRepository
        .create(participantId, createCredentialType)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()

      // test with data
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.getCredentialTypes(request)
        result.credentialTypes.size mustBe 1
        result.credentialTypes.head.name mustBe "credenital-type"
      }
    }

    "return credential type" in new Fixtures {
      // create credential type
      val credentialType =
        credentialTypeRepository
          .create(participantId, createCredentialType)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .getOrElse(fail())

      val request =
        console_api.GetCredentialTypeRequest(credentialTypeId = credentialType.credentialType.id.uuid.toString)

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.getCredentialType(request).credentialType.value
        result.credentialType.map(_.name) mustBe Some("credenital-type")
      }
    }

    "fail to get for a nonexistent id" in new Fixtures {
      val request = console_api.GetCredentialTypeRequest(credentialTypeId = UUID.randomUUID.toString)

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        stub.getCredentialType(request).credentialType mustBe None
      }
    }

    "fail to get for a invalid uuid" in new Fixtures {
      val request =
        console_api.GetCredentialTypeRequest(credentialTypeId = "invalid uuid")

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        intercept[StatusRuntimeException] {
          stub.getCredentialType(request)
        }
      }
    }

    "create credential type" in new Fixtures {
      val model = console_models.CreateCredentialType(
        name = "credenital-type"
      )
      val request =
        console_api.CreateCredentialTypeRequest(credentialType = Some(model))

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.createCredentialType(request).credentialType.value
        result.credentialType.map(_.state) mustBe Some(
          console_models.CredentialTypeState.CREDENTIAL_TYPE_DRAFT
        )
      }
    }

    "update credential type" in new Fixtures {
      // create credential type
      val credentialType =
        credentialTypeRepository
          .create(participantId, createCredentialType)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .getOrElse(fail())

      val model = console_models.UpdateCredentialType(
        id = credentialType.credentialType.id.uuid.toString,
        name = "credenital-type-changed",
        fields = Seq(
          console_models.CreateCredentialTypeField(
            name = "new field",
            description = "description",
            `type` = console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_STRING
          )
        )
      )
      val request =
        console_api.UpdateCredentialTypeRequest(credentialType = Some(model))

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      )(_.updateCredentialType(request))
      val result = credentialTypeRepository
        .find(credentialType.credentialType.id)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
      result.map(_.credentialType.name) mustBe Some("credenital-type-changed")
      val fields = result.map(_.requiredFields)
      fields.map(_.size) mustBe Some(1) // fields were overwritten
      fields.map(_.head.name) mustBe Some("new field")
    }

    "update credential type with invalid uuid" in new Fixtures {
      val model = console_models.UpdateCredentialType(
        id = "invalid uuid",
        name = "credenital-type-changed",
        fields = Seq(console_models.CreateCredentialTypeField(name = "new field"))
      )
      val request =
        console_api.UpdateCredentialTypeRequest(credentialType = Some(model))

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        intercept[StatusRuntimeException] {
          stub.updateCredentialType(request)
        }
      }
    }

    "update credential type state to ready" in new Fixtures {
      // create credential type
      val credentialType =
        credentialTypeRepository
          .create(participantId, createCredentialType)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .getOrElse(fail())

      val request =
        console_api.MarkAsReadyCredentialTypeRequest(credentialTypeId = credentialType.credentialType.id.uuid.toString)

      credentialType.credentialType.state mustBe CredentialTypeState.Draft

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      )(_.markAsReadyCredentialType(request))
      credentialTypeRepository
        .find(credentialType.credentialType.id)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .map(_.credentialType.state) mustBe Some(CredentialTypeState.Ready)
    }

    "update credential type state to archived" in new Fixtures {
      // create credential type
      val credentialType =
        credentialTypeRepository
          .create(participantId, createCredentialType)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .getOrElse(fail())

      val request =
        console_api.MarkAsArchivedCredentialTypeRequest(credentialTypeId =
          credentialType.credentialType.id.uuid.toString
        )

      credentialType.credentialType.state mustBe CredentialTypeState.Draft

      // test
      usingApiAsCredentialType(
        SignedRpcRequest.generate(keyPair, did, request)
      )(
        _.markAsArchivedCredentialType(request)
      )
      credentialTypeRepository
        .find(credentialType.credentialType.id)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .map(_.credentialType.state) mustBe Some(CredentialTypeState.Archived)
    }

  }

  trait Fixtures {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.getPublicKey)
    val participantId = createParticipant("Institution", did)

    val createCredentialType = sampleCreateCredentialType(
      name = "credenital-type"
    ).copy(fields =
      List(
        CreateCredentialTypeField(
          name = "field",
          description = "field description",
          `type` = CredentialTypeFieldType.String
        )
      )
    )
  }
}
