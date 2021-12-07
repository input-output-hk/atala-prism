package io.iohk.atala.prism.vault.services

import cats.effect.unsafe.implicits.global
import com.google.protobuf.ByteString
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.protos.{common_models, vault_api}
import io.iohk.atala.prism.vault.TestUtils.{createRequest, randomRecordId, randomRecordType}
import io.iohk.atala.prism.vault.VaultRpcSpecBase
import org.scalatest.OptionValues

class EncryptedDataVaultGrpcServiceSpec extends VaultRpcSpecBase with OptionValues {

  "health check" should {
    "respond" in {
      val response = vaultGrpcService
        .healthCheck(common_models.HealthCheckRequest())
        .futureValue
      response must be(common_models.HealthCheckResponse())
    }
  }

  "store" should {
    "create a payload" in {
      val type_ = randomRecordType()
      val payload = "encrypted_data"
      val (request, _) = createRequest(payload, type_)

      usingApiAs.unlogged { serviceStub =>
        val responseRecord = serviceStub.storeRecord(request).record.get

        val storedPayloads =
          recordsRepository
            .getRecordsPaginated(type_, None, 10)
            .run(TraceId.generateYOLO)
            .unsafeRunSync()

        storedPayloads.size must be(1)
        val storedPayload = storedPayloads.head

        storedPayload.id.encrypted must be(responseRecord.id.toByteArray.toVector)
        storedPayload.type_.encrypted must be(responseRecord.`type`.toByteArray.toVector)
      }
    }

    "fail on the multiple different requests with the same id" in {
      val recordId = randomRecordId()
      val type1 = randomRecordType()
      val type2 = randomRecordType()
      val (request1, _) = createRequest(type1, recordId, "encrypted_data_1")
      val (request2, _) = createRequest(type2, recordId, "encrypted_data_2")

      usingApiAs.unlogged { serviceStub =>
        serviceStub.storeRecord(request1)
      }

      usingApiAs.unlogged { serviceStub =>
        intercept[RuntimeException] {
          serviceStub.storeRecord(request2)
        }
      }
    }
  }

  "get" should {
    "return all created payloads" in {
      val recordType = randomRecordType()
      val payload1 = "encrypted_data_1"
      val (request1, _) = createRequest(payload1, recordType)

      val id1 = usingApiAs.unlogged { serviceStub =>
        serviceStub.storeRecord(request1).record.get.id
      }

      val payload2 = "encrypted_data_1"
      val (request2, _) = createRequest(payload2, recordType)
      val id2 = usingApiAs.unlogged { serviceStub =>
        serviceStub.storeRecord(request2).record.get.id
      }

      val request3 =
        vault_api.GetRecordsPaginatedRequest(
          `type` = ByteString.copyFrom(recordType.encrypted.toArray),
          limit = 5
        )
      usingApiAs.unlogged { serviceStub =>
        val records1 = serviceStub
          .getRecordsPaginated(request3)
          .records

        records1.size must be(2)
        assert(records1.exists(_.id == id1))
        assert(records1.exists(_.id == id2))
      }

      val request4 =
        vault_api.GetRecordsPaginatedRequest(`type` = ByteString.copyFrom(recordType.encrypted.toArray), limit = 1)
      val records2 = usingApiAs.unlogged { serviceStub =>
        val records2 = serviceStub
          .getRecordsPaginated(request4)
          .records

        records2.size must be(1)
        assert(records2.exists(_.id == id1))
        records2
      }

      val request5 = vault_api.GetRecordsPaginatedRequest(
        lastSeenId = records2.head.id,
        `type` = ByteString.copyFrom(recordType.encrypted.toArray),
        limit = 1
      )
      usingApiAs.unlogged { serviceStub =>
        val records3 = serviceStub
          .getRecordsPaginated(request5)
          .records

        records3.size must be(1)
        assert(records3.exists(_.id == id2))
      }
    }
  }
}
