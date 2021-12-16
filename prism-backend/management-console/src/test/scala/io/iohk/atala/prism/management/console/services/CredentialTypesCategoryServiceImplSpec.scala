package io.iohk.atala.prism.management.console.services

import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.management.console.DataPreparation._
import io.iohk.atala.prism.management.console.ManagementConsoleRpcSpecBase
import io.iohk.atala.prism.management.console.models.{CreateCredentialTypeCategory, CredentialTypeCategoryState}
import io.iohk.atala.prism.protos.{console_api, console_models}
import org.scalatest.OptionValues

class CredentialTypesCategoryServiceImplSpec extends ManagementConsoleRpcSpecBase with DIDUtil with OptionValues {

  "CredentialTypeCategoriesServiceImpl" should {

    "return all credential type categories of a given participant" in new Fixtures {
      val request = console_api.GetCredentialTypeCategoriesRequest()

      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)

      val created = credentialTypeCategoryRepository
        .create(participantId, createCredentialTypeCategory1)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      usingApiAsCredentialTypeCategory(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.getCredentialTypeCategories(request)
        result.credentialTypeCategories mustBe Seq(created).map(ProtoCodecs.toCredentialTypeCategoryProto)
      }
    }

    "don't return any credential type categories for participant that does not have any" in new Fixtures {
      val request = console_api.GetCredentialTypeCategoriesRequest()

      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)

      credentialTypeCategoryRepository
        .create(participantId, createCredentialTypeCategory1)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      val anotherKeyPair = EC.generateKeyPair()
      val anotherDid = generateDid(anotherKeyPair.getPublicKey)
      createParticipant("Institution", anotherDid)

      usingApiAsCredentialTypeCategory(
        SignedRpcRequest.generate(anotherKeyPair, anotherDid, request)
      ) { stub =>
        val result = stub.getCredentialTypeCategories(request)
        result.credentialTypeCategories mustBe Seq.empty[console_models.CredentialTypeCategory]
      }
    }

    "create a credential type" in new Fixtures {
      val createCredentialTypeCategory = console_models.CreateCredentialTypeCategory(
        "some category",
        console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_READY
      )

      val request = console_api.CreateCredentialTypeCategoryRequest(
        Some(
          createCredentialTypeCategory
        )
      )

      usingApiAsCredentialTypeCategory(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.createCredentialTypeCategory(request)

        val inDb = credentialTypeCategoryRepository
          .findByInstitution(participantId)
          .run(TraceId.generateYOLO)
          .unsafeRunSync()
          .toOption
          .value
          .head

        result.credentialTypeCategory.value mustBe ProtoCodecs.toCredentialTypeCategoryProto(inDb)
      }
    }

    "archive existing credential type category" in new Fixtures {

      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Ready)
      val created = credentialTypeCategoryRepository
        .create(participantId, createCredentialTypeCategory1)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      created.state mustBe CredentialTypeCategoryState.Ready

      val request = console_api.ArchiveCredentialTypeCategoryRequest(created.id.toString)

      usingApiAsCredentialTypeCategory(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.archiveCredentialTypeCategory(request)
        result.credentialTypeCategory.value.state mustBe console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_ARCHIVED
      }
    }

    "uarchive existing credential type category" in new Fixtures {

      val createCredentialTypeCategory1 =
        CreateCredentialTypeCategory("some category1", CredentialTypeCategoryState.Archived)
      val created = credentialTypeCategoryRepository
        .create(participantId, createCredentialTypeCategory1)
        .run(TraceId.generateYOLO)
        .unsafeRunSync()
        .toOption
        .value

      created.state mustBe CredentialTypeCategoryState.Archived

      val request = console_api.UnArchiveCredentialTypeCategoryRequest(created.id.toString)

      usingApiAsCredentialTypeCategory(
        SignedRpcRequest.generate(keyPair, did, request)
      ) { stub =>
        val result = stub.unArchiveCredentialTypeCategory(request)
        result.credentialTypeCategory.value.state mustBe console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_READY
      }
    }

  }

  trait Fixtures {
    val keyPair = EC.generateKeyPair()
    val did = generateDid(keyPair.getPublicKey)
    val participantId = createParticipant("Institution", did)
  }
}
