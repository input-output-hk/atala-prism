package io.iohk.atala.prism.node.services

import doobie.implicits._
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.operations.{CreateDIDOperation, CreateDIDOperationSpec}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import org.scalatest.OptionValues._

import java.time.Instant

class DIDDataServiceSpec extends AtalaWithPostgresSpec {
  import CreateDIDOperationSpec._

  lazy val didDataRepository = new DIDDataRepository(database)
  lazy val didDataService = new DIDDataService(didDataRepository)
  val dummyTimestamp = TimestampInfo(Instant.ofEpochMilli(0), 1, 0)
  val dummyLedgerData = LedgerData(
    TransactionId.from(Array.fill[Byte](TransactionId.config.size.toBytes.toInt)(0)).value,
    Ledger.InMemory,
    dummyTimestamp
  )

  "DIDDataServiceSpec.findByDID" should {
    "retrieve DID document from database" in {
      val parsedOperation = CreateDIDOperation.parse(exampleOperation, dummyLedgerData).toOption.value

      val result = parsedOperation
        .applyState()
        .transact(database)
        .value
        .unsafeToFuture()
        .futureValue
      result mustBe a[Right[_, _]]

      val did = DID.buildPrismDID(parsedOperation.id.value)
      didDataService.findByDID(did).value.futureValue mustBe a[Right[_, _]]
    }

    "return error when did is in invalid format" in {
      val did = DID.buildPrismDID("1111111111111111")
      didDataService.findByDID(did).value.futureValue mustBe a[Left[UnknownValueError, _]]
    }

  }

}
